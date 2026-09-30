package com.apiatlas.dto;

import java.util.List;

public record DuplicateGroupDto(String key, double similarity, List<ApiEndpointDto> endpoints) {
}
