package com.apiatlas.service;

import com.apiatlas.ai.AiAnalysisService;
import com.apiatlas.config.AtlasProperties;
import com.apiatlas.crawler.MobileExplorer;
import com.apiatlas.crawler.WebCrawler;
import com.apiatlas.dto.StartDiscoveryRequest;
import com.apiatlas.interceptor.TrafficPublisher;
import com.apiatlas.model.DiscoverySession;
import com.apiatlas.model.Enums.SessionStatus;
import com.apiatlas.model.Enums.SessionType;
import com.apiatlas.repository.DiscoverySessionRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Orchestrates discovery sessions (web crawling / mobile exploration) with bounded concurrency. */
@Service
@Slf4j
public class DiscoveryService {
    private final AtlasProperties props;
    private final DiscoverySessionRepository sessions;
    private final WebCrawler webCrawler;
    private final MobileExplorer mobileExplorer;
    private final TrafficIngestionService ingestion;
    private final AiAnalysisService ai;
    private final TrafficPublisher publisher;
    private final ExecutorService pool;
    private final Map<Long, AtomicBoolean> running = new ConcurrentHashMap<>();
    private final List<String> allowedHosts;
    private final boolean allowPrivateTargets;

    public DiscoveryService(AtlasProperties props, DiscoverySessionRepository sessions, WebCrawler webCrawler,
                            MobileExplorer mobileExplorer, TrafficIngestionService ingestion, AiAnalysisService ai,
                            TrafficPublisher publisher,
                            @org.springframework.beans.factory.annotation.Value("${ATLAS_ALLOWED_HOSTS:}") String allowed,
                            @org.springframework.beans.factory.annotation.Value("${ATLAS_ALLOW_PRIVATE_TARGETS:false}") boolean allowPrivate) {
        this.allowPrivateTargets = allowPrivate;
        this.allowedHosts = java.util.Arrays.stream(allowed.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(java.util.Locale.ROOT)).toList();
        this.props = props;
        this.sessions = sessions;
        this.webCrawler = webCrawler;
        this.mobileExplorer = mobileExplorer;
        this.ingestion = ingestion;
        this.ai = ai;
        this.publisher = publisher;
        this.pool = Executors.newFixedThreadPool(Math.max(1, props.discovery().maxConcurrentSessions()));
    }

    public synchronized DiscoverySession start(StartDiscoveryRequest req) {
        if (running.size() >= props.discovery().maxConcurrentSessions()) {
            throw new IllegalStateException("Maximum number of concurrent discovery sessions reached");
        }
        validate(req);
        DiscoverySession s = new DiscoverySession();
        s.setName(req.name() != null && !req.name().isBlank() ? req.name() : "Discovery " + Instant.now());
        s.setTarget(req.target());
        s.setType(req.type());
        s.setStatus(SessionStatus.RUNNING);
        s.setBrowser(req.browser());
        s.setPlatform(req.platform());
        s.setStartedAt(Instant.now());
        DiscoverySession saved = sessions.save(s);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        running.put(saved.getId(), cancelled);
        pool.submit(() -> run(saved.getId(), req, cancelled));
        return saved;
    }

    public DiscoverySession stop(Long sessionId) {
        AtomicBoolean flag = running.get(sessionId);
        if (flag == null) {
            throw new NoSuchElementException("Session " + sessionId + " is not running");
        }
        flag.set(true);
        return sessions.findById(sessionId).orElseThrow();
    }

    public List<DiscoverySession> list() {
        return sessions.findAllByOrderByStartedAtDesc();
    }

    private void run(Long id, StartDiscoveryRequest req, AtomicBoolean cancelled) {
        SessionStatus finalStatus = SessionStatus.COMPLETED;
        String error = null;
        int progress = 0;
        try {
            progress = req.type() == SessionType.WEB
                    ? webCrawler.crawl(id, req, cancelled::get)
                    : mobileExplorer.explore(id, req, cancelled::get);
            if (cancelled.get()) finalStatus = SessionStatus.STOPPED;
        } catch (Exception ex) {
            log.error("Discovery session {} failed", id, ex);
            finalStatus = SessionStatus.FAILED;
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        } finally {
            ingestion.endSession(id);
            running.remove(id);
        }
        int pages = progress;
        SessionStatus status = finalStatus;
        String err = error;
        sessions.findById(id).ifPresent(s -> {
            s.setStatus(status);
            s.setPagesVisited(pages);
            s.setErrorMessage(err);
            s.setEndedAt(Instant.now());
            sessions.save(s);
        });
        try {
            if (!publisher.analysisRequested(id)) {
                ai.enrichPending();
            }
        } catch (Exception ex) {
            log.warn("Post-discovery analysis failed: {}", ex.getMessage());
        }
    }

    private void validate(StartDiscoveryRequest req) {
        if (req.type() != SessionType.WEB) {
            MobileExplorer.validate(req);
        }
        if (req.type() == SessionType.WEB) {
            URI u;
            try {
                u = URI.create(req.target());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Target must be a valid URL");
            }
            if (u.getHost() == null || !("http".equals(u.getScheme()) || "https".equals(u.getScheme()))) {
                throw new IllegalArgumentException("Web target must be an http(s) URL");
            }
            if (!allowPrivateTargets && com.apiatlas.utility.UrlGuard.isInternal(u.getHost())) {
                throw new IllegalArgumentException(
                        "Target resolves to an internal address (set ATLAS_ALLOW_PRIVATE_TARGETS=true to allow in test environments)");
            }
            if (!allowedHosts.isEmpty()) {
                String host = u.getHost().toLowerCase(java.util.Locale.ROOT);
                boolean ok = allowedHosts.stream().anyMatch(a -> host.equals(a) || host.endsWith("." + a));
                if (!ok) {
                    throw new IllegalArgumentException("Target host is not in ATLAS_ALLOWED_HOSTS");
                }
            }
        }
    }

    @PreDestroy
    void shutdown() {
        running.values().forEach(f -> f.set(true));
        pool.shutdown();
    }
}
