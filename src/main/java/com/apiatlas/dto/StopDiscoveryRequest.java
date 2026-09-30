package com.apiatlas.dto;

import jakarta.validation.constraints.NotNull;

public record StopDiscoveryRequest(@NotNull Long sessionId) {
}
