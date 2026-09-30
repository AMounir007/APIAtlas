package com.apiatlas.dto;

import com.apiatlas.model.Enums.Protocol;

import java.util.Map;

/** A single captured HTTP(S)/WS transaction, produced by the crawler, mitmproxy addon or Kafka. */
public record CapturedTraffic(
        Long sessionId,
        String url,
        String method,
        Map<String, String> requestHeaders,
        String requestBody,
        Integer statusCode,
        Map<String, String> responseHeaders,
        String responseBody,
        String contentType,
        Long responseTimeMs,
        Protocol protocol) {
}
