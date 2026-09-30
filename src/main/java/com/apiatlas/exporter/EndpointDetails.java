package com.apiatlas.exporter;

import com.apiatlas.model.ApiRequest;
import com.apiatlas.model.ApiResponse;
import com.apiatlas.repository.ApiRequestRepository;
import com.apiatlas.repository.ApiResponseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Read helpers shared by all exporters (latest request data, per-status response examples). */
@Component
@RequiredArgsConstructor
public class EndpointDetails {
    private final ApiRequestRepository requests;
    private final ApiResponseRepository responses;
    private final ObjectMapper mapper;

    /** Latest response per status code (most recent first). */
    public Map<Integer, ApiResponse> responsesByStatus(Long endpointId) {
        Map<Integer, ApiResponse> out = new LinkedHashMap<>();
        for (ApiResponse r : responses.findTop50ByEndpointIdOrderByCapturedAtDesc(endpointId)) {
            out.putIfAbsent(r.getStatusCode(), r);
        }
        return out;
    }

    public Optional<ApiRequest> latestRequest(Long endpointId) {
        return requests.findFirstByEndpointIdOrderByCapturedAtDesc(endpointId);
    }

    public Map<String, String> queryParams(Long endpointId) {
        return latestRequest(endpointId).map(r -> readMap(r.getQueryParams())).orElseGet(LinkedHashMap::new);
    }

    public Map<String, String> requestHeaders(Long endpointId) {
        return latestRequest(endpointId).map(r -> readMap(r.getHeaders())).orElseGet(LinkedHashMap::new);
    }

    public Map<String, String> readMap(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return mapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    /** Parses JSON samples into a tree so they are embedded as objects; falls back to the raw string. */
    public Object example(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode n = mapper.readTree(body);
            return n.isContainerNode() ? n : body;
        } catch (Exception ex) {
            return body;
        }
    }

    public boolean isJson(String body) {
        return example(body) instanceof JsonNode;
    }
}
