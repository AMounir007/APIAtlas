package com.apiatlas.service;

import com.apiatlas.analyzer.ApiAnalyzer;
import com.apiatlas.dto.*;
import com.apiatlas.model.*;
import com.apiatlas.model.Enums.EndpointStatus;
import com.apiatlas.model.Enums.SessionStatus;
import com.apiatlas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    private final ApiEndpointRepository endpoints;
    private final DiscoverySessionRepository sessions;
    private final SecurityFindingRepository findings;
    private final ApiStatisticsRepository statistics;
    private final ApiRelationshipRepository relationships;
    private final ApiAnalyzer analyzer;

    public StatisticsDto summary() {
        long duplicates = analyzer.findDuplicates().stream().mapToLong(g -> g.endpoints().size()).sum();
        return new StatisticsDto(
                endpoints.count(),
                endpoints.countByStatus(EndpointStatus.ACTIVE),
                endpoints.countByStatus(EndpointStatus.DEPRECATED),
                duplicates,
                endpoints.countByStatus(EndpointStatus.UNUSED),
                endpoints.countByThirdPartyTrue(),
                sessions.count(),
                sessions.countByStatus(SessionStatus.RUNNING),
                findings.countByResolvedFalse(),
                toMap(endpoints.countByAuthType()),
                toMap(endpoints.countByProtocol()),
                toMap(endpoints.countByService()),
                toMap(findings.countBySeverity()));
    }

    public SecurityReportDto securityReport() {
        return new SecurityReportDto(Instant.now(), findings.countByResolvedFalse(), toMap(findings.countBySeverity()),
                toMap(findings.countByCategory()), findings.findByResolvedFalseOrderByDetectedAtDesc());
    }

    public Map<String, List<TrafficRowDto>> traffic() {
        Map<String, List<TrafficRowDto>> out = new LinkedHashMap<>();
        out.put("mostCalled", rows(statistics.findTop10ByOrderByCallCountDesc()));
        out.put("slowest", rows(statistics.findTop10ByOrderByAvgResponseMsDesc()));
        out.put("mostErrors", rows(statistics.findTop10ByOrderByErrorCountDesc()));
        return out;
    }

    public DependencyGraphDto dependencyGraph() {
        Map<Long, ApiEndpoint> byId = new HashMap<>();
        endpoints.findAll().forEach(e -> byId.put(e.getId(), e));
        Map<String, long[]> nodes = new LinkedHashMap<>();
        Map<String, Boolean> third = new HashMap<>();
        byId.values().forEach(e -> {
            nodes.computeIfAbsent(e.getService(), k -> new long[1])[0]++;
            third.merge(e.getService(), e.isThirdParty(), (a, b) -> a || b);
        });
        Map<String, Long> edges = new LinkedHashMap<>();
        for (ApiRelationship r : relationships.findAll()) {
            ApiEndpoint s = byId.get(r.getSourceEndpointId());
            ApiEndpoint t = byId.get(r.getTargetEndpointId());
            if (s == null || t == null || s.getService().equals(t.getService())) continue;
            edges.merge(s.getService() + "\u0000" + t.getService(), r.getWeight(), Long::sum);
        }
        List<DependencyGraphDto.Node> nl = nodes.entrySet().stream()
                .map(en -> new DependencyGraphDto.Node(en.getKey(), en.getValue()[0], third.getOrDefault(en.getKey(), false))).toList();
        List<DependencyGraphDto.Edge> el = edges.entrySet().stream().map(en -> {
            String[] p = en.getKey().split("\u0000");
            return new DependencyGraphDto.Edge(p[0], p[1], en.getValue());
        }).toList();
        return new DependencyGraphDto(nl, el);
    }

    private List<TrafficRowDto> rows(List<ApiStatistics> stats) {
        Map<Long, ApiEndpoint> byId = new HashMap<>();
        endpoints.findAllById(stats.stream().map(ApiStatistics::getEndpointId).toList()).forEach(e -> byId.put(e.getId(), e));
        return stats.stream().map(s -> {
            ApiEndpoint e = byId.get(s.getEndpointId());
            String label = e == null ? "#" + s.getEndpointId() : e.getMethod() + " " + e.getService() + e.getPath();
            return new TrafficRowDto(s.getEndpointId(), label, s.getCallCount(), s.getErrorCount(),
                    Math.round(s.getAvgResponseMs() * 10.0) / 10.0, s.getMaxResponseMs());
        }).toList();
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] r : rows) m.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        return m;
    }
}
