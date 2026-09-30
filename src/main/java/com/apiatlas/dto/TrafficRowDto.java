package com.apiatlas.dto;

public record TrafficRowDto(Long endpointId, String endpoint, long calls, long errors, double avgResponseMs,
                            long maxResponseMs) {
}
