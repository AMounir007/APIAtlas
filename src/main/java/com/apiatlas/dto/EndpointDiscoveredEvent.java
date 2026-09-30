package com.apiatlas.dto;

/** Kafka payload published when a new endpoint enters the catalog. */
public record EndpointDiscoveredEvent(Long id, String method, String url, String service) {
}
