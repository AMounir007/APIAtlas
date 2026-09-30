package com.apiatlas.dto;

import java.util.Map;

public record StatisticsDto(
        long totalApis, long activeApis, long deprecatedApis, long duplicateApis, long unusedApis,
        long thirdPartyApis, long discoverySessions, long runningSessions, long openFindings,
        Map<String, Long> authTypes, Map<String, Long> protocols, Map<String, Long> services,
        Map<String, Long> findingsBySeverity) {
}
