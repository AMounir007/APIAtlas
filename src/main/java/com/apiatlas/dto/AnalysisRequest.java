package com.apiatlas.dto;

/** Kafka payload requesting AI enrichment; {@code sessionId} may be null for "all pending". */
public record AnalysisRequest(Long sessionId) {
}
