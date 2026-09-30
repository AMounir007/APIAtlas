package com.apiatlas.analyzer;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAnalyzerTest {
    @Test
    void versionsAreIgnoredWhenComparingPaths() {
        Set<String> a = ApiAnalyzer.segments("/api/v1/users/{id}");
        Set<String> b = ApiAnalyzer.segments("/api/v2/users/{id}");
        assertEquals(1.0, ApiAnalyzer.similarity(a, b));
    }

    @Test
    void differentPathsHaveLowSimilarity() {
        double s = ApiAnalyzer.similarity(ApiAnalyzer.segments("/users/{id}"), ApiAnalyzer.segments("/users/{id}/orders"));
        assertTrue(s < 0.9);
    }
}
