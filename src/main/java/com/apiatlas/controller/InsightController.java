package com.apiatlas.controller;

import com.apiatlas.analyzer.ApiAnalyzer;
import com.apiatlas.dto.*;
import com.apiatlas.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InsightController {
    private final StatisticsService statistics;
    private final ApiAnalyzer analyzer;

    @GetMapping("/api/statistics")
    public StatisticsDto statistics() {
        return statistics.summary();
    }

    @GetMapping("/api/statistics/traffic")
    public Map<String, List<TrafficRowDto>> traffic() {
        return statistics.traffic();
    }

    @GetMapping("/api/security")
    public SecurityReportDto security() {
        return statistics.securityReport();
    }

    @GetMapping("/api/duplicates")
    public List<DuplicateGroupDto> duplicates() {
        return analyzer.findDuplicates();
    }

    @GetMapping("/api/map/services")
    public DependencyGraphDto serviceMap() {
        return statistics.dependencyGraph();
    }
}
