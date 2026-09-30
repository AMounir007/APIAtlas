package com.apiatlas.utility;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    @Test
    void normalizesDynamicSegments() {
        assertEquals("/api/users/{id}/orders/{uuid}",
                PathNormalizer.normalize("/api/users/42/orders/123e4567-e89b-12d3-a456-426614174000"));
        assertEquals("/a/{id}/b/{id2}", PathNormalizer.normalize("/a/1/b/2"));
        assertEquals("/", PathNormalizer.normalize(""));
        assertEquals("/api/users", PathNormalizer.normalize("/api/users"));
    }

    @Test
    void extractsVersionAndModule() {
        assertEquals("v2", PathNormalizer.version("/api/v2/users"));
        assertNull(PathNormalizer.version("/api/users"));
        assertEquals("/api/users", PathNormalizer.stripVersion("/api/v2/users"));
        assertEquals("users", PathNormalizer.module("/api/v2/users/{id}"));
        assertEquals("root", PathNormalizer.module("/"));
    }

    @Test
    void derivesBaseUrl() {
        assertEquals("https://h.example.com", PathNormalizer.baseUrl("https://h.example.com/api/x", "/api/x"));
    }

    @Test
    void masksSecretsInBodiesHeadersAndParams() {
        String masked = Masker.maskBody("{\"user\":\"bob\",\"password\":\"s3cr3t\",\"access_token\":\"abc\"}");
        assertFalse(masked.contains("s3cr3t"));
        assertFalse(masked.contains("abc"));
        assertTrue(masked.contains("bob"));
        assertFalse(Masker.maskBody("a=1&password=hunter2").contains("hunter2"));

        Map<String, String> headers = Masker.maskHeaders(Map.of("Authorization", "Bearer x", "Accept", "json"), List.of("authorization"));
        assertEquals(Masker.MASK, headers.get("Authorization"));
        assertEquals("json", headers.get("Accept"));

        assertEquals(Masker.MASK, Masker.maskParams(Map.of("api_key", "k")).get("api_key"));
    }
}
