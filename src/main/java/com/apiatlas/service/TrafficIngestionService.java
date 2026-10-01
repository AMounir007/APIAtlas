package com.apiatlas.service;

import com.apiatlas.analyzer.SecurityAnalyzer;
import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.CapturedTraffic;
import com.apiatlas.interceptor.TrafficPublisher;
import com.apiatlas.model.*;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.Protocol;
import com.apiatlas.repository.*;
import com.apiatlas.utility.Masker;
import com.apiatlas.utility.PathNormalizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** Turns captured traffic into catalog entries, history, statistics, relationships and security findings. */
@Service
@Slf4j
public class TrafficIngestionService {
    private static final int HISTORY_LIMIT = 25;

    private final AtlasProperties props;
    private final ApiEndpointRepository endpoints;
    private final ApiRequestRepository requests;
    private final ApiResponseRepository responses;
    private final ApiStatisticsRepository statistics;
    private final ApiRelationshipRepository relationships;
    private final DiscoverySessionRepository sessions;
    private final SecurityAnalyzer securityAnalyzer;
    private final TrafficPublisher publisher;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final Counter ingested;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<Long, Long> lastEndpointBySession = new ConcurrentHashMap<>();

    public TrafficIngestionService(AtlasProperties props, ApiEndpointRepository endpoints, ApiRequestRepository requests,
                                   ApiResponseRepository responses, ApiStatisticsRepository statistics,
                                   ApiRelationshipRepository relationships, DiscoverySessionRepository sessions,
                                   SecurityAnalyzer securityAnalyzer, TrafficPublisher publisher, ObjectMapper mapper,
                                   PlatformTransactionManager txManager, MeterRegistry meters) {
        this.props = props;
        this.endpoints = endpoints;
        this.requests = requests;
        this.responses = responses;
        this.statistics = statistics;
        this.relationships = relationships;
        this.sessions = sessions;
        this.securityAnalyzer = securityAnalyzer;
        this.publisher = publisher;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(txManager);
        this.ingested = Counter.builder("apiatlas.traffic.ingested").description("Captured transactions ingested").register(meters);
    }

    private record Result(ApiEndpoint endpoint, boolean created) {}

    public Optional<ApiEndpoint> ingest(CapturedTraffic t) {
        if (t == null || t.url() == null || t.method() == null) {
            return Optional.empty();
        }
        lock.lock();
        try {
            Result result;
            try {
                result = tx.execute(s -> doIngest(t));
            } catch (DataIntegrityViolationException ex) {
                log.debug("Concurrent insert detected, retrying once");
                result = tx.execute(s -> doIngest(t));
            }
            if (result == null) {
                return Optional.empty();
            }
            ingested.increment();
            if (result.created()) {
                publisher.endpointDiscovered(result.endpoint());
            }
            return Optional.of(result.endpoint());
        } catch (Exception ex) {
            log.warn("Failed to ingest {} {}: {}", t.method().replaceAll("[\\p{Cntrl}]", "_"),
                    t.url().replaceAll("[\\p{Cntrl}]", "_"), ex.getClass().getSimpleName());
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public void endSession(Long sessionId) {
        if (sessionId != null) {
            lastEndpointBySession.remove(sessionId);
        }
    }

    private Result doIngest(CapturedTraffic t) {
        URI uri;
        try {
            uri = URI.create(t.url().trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (uri.getHost() == null || uri.getScheme() == null) {
            return null;
        }
        String rawPath = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) ? "/" : uri.getRawPath();
        String method = t.method().toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method) || isExcluded(rawPath)) {
            return null;
        }
        var capture = props.discovery().capture();
        String service = abbreviate(uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""), 255);
        String path = abbreviate(PathNormalizer.normalize(rawPath), 1000);
        Map<String, String> reqH = lower(t.requestHeaders());
        Map<String, String> respH = lower(t.responseHeaders());
        int status = t.statusCode() == null ? 0 : t.statusCode();
        long responseTime = t.responseTimeMs() == null ? 0 : Math.max(0, t.responseTimeMs());
        Map<String, String> query = parseQuery(uri.getRawQuery());
        Instant now = Instant.now();

        Optional<ApiEndpoint> existing = endpoints.findByMethodAndServiceAndPath(method, service, path);
        boolean created = existing.isEmpty();
        ApiEndpoint e = existing.orElseGet(ApiEndpoint::new);
        if (created) {
            e.setSessionId(t.sessionId());
            e.setMethod(method);
            e.setService(service);
            e.setPath(path);
            e.setUrl(abbreviate(uri.getScheme() + "://" + service + path, 2048));
            e.setName(abbreviate(method + " " + path, 255));
            e.setModule(PathNormalizer.module(path));
            String version = PathNormalizer.version(path);
            e.setVersion(version != null ? version : firstNonBlank(respH.get("api-version"), reqH.get("x-api-version"), reqH.get("api-version")));
            e.setProtocol(detectProtocol(t, reqH, respH, path));
            e.setAuthType(AuthType.NONE);
            e.setStatus(EndpointStatus.ACTIVE);
            e.setFirstSeen(now);
            e.setThirdParty(isThirdParty(t.sessionId(), uri.getHost()));
        }
        e.setLastSeen(now);
        e.setHitCount(e.getHitCount() + 1);
        AuthType detected = detectAuth(reqH);
        if (e.getAuthType() == AuthType.NONE) {
            e.setAuthType(detected);
        }
        if (isDeprecated(respH, rawPath)) {
            e.setStatus(EndpointStatus.DEPRECATED);
        } else if (e.getStatus() == EndpointStatus.UNUSED) {
            e.setStatus(EndpointStatus.ACTIVE);
        }
        int max = capture.maxBodySize();
        if (e.getSampleRequest() == null && notBlank(t.requestBody())) {
            e.setSampleRequest(Masker.maskBody(Masker.truncate(t.requestBody(), max)));
        }
        if (e.getSampleResponse() == null && status >= 200 && status < 300 && notBlank(t.responseBody())) {
            e.setSampleResponse(Masker.maskBody(Masker.truncate(t.responseBody(), max)));
        }
        e = endpoints.save(e);

        ApiRequest savedRequest = null;
        if (requests.countByEndpointId(e.getId()) < HISTORY_LIMIT) {
            ApiRequest r = new ApiRequest();
            r.setEndpointId(e.getId());
            r.setHeaders(json(Masker.maskHeaders(reqH, capture.maskedHeaders())));
            r.setQueryParams(json(Masker.maskParams(query)));
            r.setCookies(json(cookieNames(reqH.get("cookie"))));
            r.setBody(Masker.maskBody(Masker.truncate(t.requestBody(), max)));
            r.setCapturedAt(now);
            savedRequest = requests.save(r);

            ApiResponse resp = new ApiResponse();
            resp.setEndpointId(e.getId());
            resp.setRequestId(savedRequest.getId());
            resp.setStatusCode(status);
            resp.setHeaders(json(Masker.maskHeaders(respH, capture.maskedHeaders())));
            resp.setBody(Masker.maskBody(Masker.truncate(t.responseBody(), max)));
            resp.setContentType(firstNonBlank(t.contentType(), respH.get("content-type")));
            resp.setResponseTimeMs(responseTime);
            resp.setCapturedAt(now);
            responses.save(resp);
        }

        updateStatistics(e.getId(), status, responseTime, now);
        securityAnalyzer.analyze(e, uri.getScheme(), query, reqH, respH, t.responseBody(), status);
        trackRelationship(t.sessionId(), e.getId());
        if (created && t.sessionId() != null) {
            sessions.incrementEndpointsFound(t.sessionId());
        }
        return new Result(e, created);
    }

    private void updateStatistics(Long endpointId, int status, long responseTime, Instant now) {
        ApiStatistics s = statistics.findByEndpointId(endpointId).orElseGet(() -> {
            ApiStatistics n = new ApiStatistics();
            n.setEndpointId(endpointId);
            return n;
        });
        long n = s.getCallCount() + 1;
        s.setAvgResponseMs((s.getAvgResponseMs() * s.getCallCount() + responseTime) / n);
        s.setCallCount(n);
        if (status >= 400 || status == 0) s.setErrorCount(s.getErrorCount() + 1);
        s.setMaxResponseMs(Math.max(s.getMaxResponseMs(), responseTime));
        s.setUpdatedAt(now);
        statistics.save(s);
    }

    private void trackRelationship(Long sessionId, Long endpointId) {
        if (sessionId == null) return;
        Long prev = lastEndpointBySession.put(sessionId, endpointId);
        if (prev == null || prev.equals(endpointId)) return;
        ApiRelationship rel = relationships
                .findBySourceEndpointIdAndTargetEndpointIdAndType(prev, endpointId, ApiRelationship.CALLED_AFTER)
                .orElseGet(() -> {
                    ApiRelationship n = new ApiRelationship();
                    n.setSourceEndpointId(prev);
                    n.setTargetEndpointId(endpointId);
                    n.setWeight(0);
                    return n;
                });
        rel.setWeight(rel.getWeight() + 1);
        relationships.save(rel);
    }

    private boolean isExcluded(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        return props.discovery().capture().excludedExtensions().stream().anyMatch(ext -> p.endsWith(ext.toLowerCase(Locale.ROOT)));
    }

    private boolean isThirdParty(Long sessionId, String host) {
        if (sessionId == null) return false;
        return sessions.findById(sessionId).map(s -> {
            try {
                String target = URI.create(s.getTarget()).getHost();
                return target != null && !registrableDomain(target).equalsIgnoreCase(registrableDomain(host));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }).orElse(false);
    }

    private static String registrableDomain(String host) {
        String[] labels = host.split("\\.");
        return labels.length <= 2 ? host : labels[labels.length - 2] + "." + labels[labels.length - 1];
    }

    private static AuthType detectAuth(Map<String, String> h) {
        String auth = h.get("authorization");
        if (auth != null) {
            String a = auth.trim().toLowerCase(Locale.ROOT);
            if (a.startsWith("basic ")) return AuthType.BASIC;
            if (a.startsWith("bearer ")) {
                String token = auth.trim().substring(7).trim();
                return token.startsWith("eyJ") && token.chars().filter(c -> c == '.').count() == 2 ? AuthType.JWT : AuthType.OAUTH2;
            }
        }
        if (h.containsKey("x-api-key") || h.containsKey("api-key")) return AuthType.API_KEY;
        return AuthType.NONE;
    }

    private static boolean isDeprecated(Map<String, String> respH, String path) {
        String p = path.toLowerCase(Locale.ROOT);
        return respH.containsKey("deprecation") || respH.containsKey("sunset")
                || p.contains("/deprecated") || p.contains("/legacy");
    }

    private static Protocol detectProtocol(CapturedTraffic t, Map<String, String> reqH, Map<String, String> respH, String path) {
        if (t.protocol() != null) return t.protocol();
        String ct = Objects.toString(firstNonBlank(t.contentType(), respH.get("content-type")), "").toLowerCase(Locale.ROOT);
        String reqCt = reqH.getOrDefault("content-type", "").toLowerCase(Locale.ROOT);
        String scheme = t.url().toLowerCase(Locale.ROOT);
        if (scheme.startsWith("ws://") || scheme.startsWith("wss://") || "websocket".equalsIgnoreCase(reqH.get("upgrade"))) return Protocol.WEBSOCKET;
        if (ct.contains("text/event-stream")) return Protocol.SSE;
        if (ct.contains("application/grpc") || reqCt.contains("application/grpc")) return Protocol.GRPC;
        if (reqH.containsKey("soapaction") || ct.contains("soap+xml") || reqCt.contains("soap+xml")) return Protocol.SOAP;
        if (path.toLowerCase(Locale.ROOT).endsWith("/graphql") || ct.contains("graphql") || reqCt.contains("graphql")) return Protocol.GRAPHQL;
        return Protocol.REST;
    }

    private static Map<String, String> lower(Map<String, String> in) {
        Map<String, String> out = new LinkedHashMap<>();
        if (in != null) in.forEach((k, v) -> {
            if (k != null) out.put(k.toLowerCase(Locale.ROOT), v);
        });
        return out;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int i = pair.indexOf('=');
            try {
                String k = URLDecoder.decode(i < 0 ? pair : pair.substring(0, i), StandardCharsets.UTF_8);
                String v = i < 0 ? "" : URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
                out.put(k, v);
            } catch (IllegalArgumentException ignored) {
                // malformed escape sequence - skip parameter
            }
        }
        return out;
    }

    private static List<String> cookieNames(String cookieHeader) {
        List<String> names = new ArrayList<>();
        if (cookieHeader == null) return names;
        for (String part : cookieHeader.split(";")) {
            int i = part.indexOf('=');
            String n = (i < 0 ? part : part.substring(0, i)).trim();
            if (!n.isEmpty()) names.add(n);
        }
        return names;
    }

    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
