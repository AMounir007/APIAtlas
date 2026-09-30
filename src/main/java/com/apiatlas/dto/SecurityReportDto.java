package com.apiatlas.dto;

import com.apiatlas.model.SecurityFinding;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SecurityReportDto(Instant generatedAt, long totalOpen, Map<String, Long> bySeverity,
                                Map<String, Long> byCategory, List<SecurityFinding> findings) {
}
