package com.apiatlas;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end check: Flyway schema vs. JPA entities, ingestion, masking, security findings, statistics and exports.
 * Requires Docker (skipped automatically when it is unavailable). Runs through failsafe ({@code mvn verify}).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IngestionFlowIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.cache.type", () -> "simple");
        r.add("spring.kafka.listener.auto-startup", () -> "false");
        r.add("apiatlas.discovery.proxy.ingest-token", () -> "it-token");
        r.add("ATLAS_ADMIN_PASSWORD", () -> "it-password");
    }

    @Autowired
    TestRestTemplate rest;

    private String token;

    @BeforeEach
    void login() {
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/auth/login",
                Map.of("username", "admin", "password", "it-password"), JsonNode.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        token = res.getBody().get("token").asText();
    }

    private HttpEntity<Object> auth(Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return new HttpEntity<>(body, h);
    }

    private JsonNode get(String path) {
        return rest.exchange(path, HttpMethod.GET, auth(null), JsonNode.class).getBody();
    }

    @Test
    void protectedEndpointsRejectAnonymousCalls() {
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity("/api/apis", String.class).getStatusCode());
    }

    @Test
    void ingestRequiresValidToken() {
        HttpHeaders h = new HttpHeaders();
        h.set("X-Ingest-Token", "wrong");
        ResponseEntity<String> res = rest.exchange("/api/traffic/ingest", HttpMethod.POST,
                new HttpEntity<>(List.of(), h), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void ingestedTrafficBecomesCatalogEntryWithFindingsAndExports() {
        Map<String, Object> tx = Map.of(
                "url", "https://api.example.com/api/v1/users/42?access_token=abc123",
                "method", "GET",
                "requestHeaders", Map.of("Authorization", "Bearer secret-token-value"),
                "statusCode", 200,
                "responseHeaders", Map.of("content-type", "application/json", "server", "nginx/1.18.0"),
                "responseBody", "{\"id\":42,\"password\":\"hunter2\"}",
                "contentType", "application/json",
                "responseTimeMs", 120);

        HttpHeaders h = new HttpHeaders();
        h.set("X-Ingest-Token", "it-token");
        ResponseEntity<JsonNode> ingest = rest.exchange("/api/traffic/ingest", HttpMethod.POST,
                new HttpEntity<>(List.of(tx, tx), h), JsonNode.class);
        assertEquals(HttpStatus.OK, ingest.getStatusCode());
        assertEquals(2, ingest.getBody().get("accepted").asInt());

        // path is normalised and both hits map to one endpoint
        JsonNode page = get("/api/apis?q=users");
        assertEquals(1, page.get("totalElements").asInt());
        JsonNode api = page.get("content").get(0);
        assertEquals("/api/v1/users/{id}", api.get("url").asText().replace("https://api.example.com", ""));
        assertEquals("v1", api.get("version").asText());
        assertEquals("GET", api.get("method").asText());
        assertEquals(2, api.get("hitCount").asInt());

        // detail: sample response is masked, never contains the raw secret
        JsonNode detail = get("/api/apis/" + api.get("id").asLong());
        String sample = detail.get("endpoint").get("sampleResponse").asText();
        assertFalse(sample.contains("hunter2"), "password must be masked");

        // security findings: token in URL, sensitive data, weak headers
        String report = get("/api/security").toString();
        assertTrue(report.contains("TOKEN_LEAKAGE"));
        assertTrue(report.contains("SENSITIVE_DATA_EXPOSURE"));
        assertTrue(report.contains("WEAK_SECURITY_HEADERS"));

        // statistics reflect the catalog
        assertTrue(get("/api/statistics").get("totalApis").asInt() >= 1);

        // every export responds with a non-empty body
        for (String f : List.of("swagger", "postman", "csv", "json", "html", "excel", "pdf", "tests", "security-report")) {
            ResponseEntity<byte[]> res = rest.exchange("/api/export/" + f, HttpMethod.GET, auth(null), byte[].class);
            assertEquals(HttpStatus.OK, res.getStatusCode(), f);
            assertNotNull(res.getBody(), f);
            assertTrue(res.getBody().length > 0, f);
        }

        // OpenAPI output is valid JSON with the normalised path and no raw secrets
        byte[] swagger = rest.exchange("/api/export/swagger", HttpMethod.GET, auth(null), byte[].class).getBody();
        String spec = new String(swagger, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(spec.contains("/api/v1/users/{id}"));
        assertFalse(spec.contains("hunter2"));
        assertFalse(spec.contains("secret-token-value"));
    }
}
