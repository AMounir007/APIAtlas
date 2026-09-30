package com.apiatlas.ai;

import com.apiatlas.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Minimal OpenAI-compatible chat-completions client. */
@Component
@Slf4j
public class AiClient {
    private final AtlasProperties.Ai config;
    private final ObjectMapper mapper;
    private final RestClient client;

    public AiClient(AtlasProperties props, ObjectMapper mapper) {
        this.config = props.ai();
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) config.timeout().toMillis();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.client = RestClient.builder().baseUrl(config.baseUrl()).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return config.enabled() && config.apiKey() != null && !config.apiKey().isBlank();
    }

    /** Returns the assistant message content, or empty on any failure. */
    public Optional<String> chatJson(String system, String user) {
        if (!isEnabled()) return Optional.empty();
        try {
            Map<String, Object> body = Map.of(
                    "model", config.model(),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)));
            String raw = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(String.class);
            JsonNode content = mapper.readTree(raw).path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? Optional.empty() : Optional.of(content.asText());
        } catch (Exception ex) {
            log.warn("AI request failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
