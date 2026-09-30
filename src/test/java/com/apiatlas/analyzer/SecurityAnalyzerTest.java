package com.apiatlas.analyzer;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.SecurityFinding;
import com.apiatlas.repository.SecurityFindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAnalyzerTest {
    private SecurityFindingRepository repo;
    private SecurityAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(SecurityFindingRepository.class);
        Mockito.when(repo.findByEndpointIdAndCategory(Mockito.anyLong(), Mockito.anyString())).thenReturn(Optional.empty());
        analyzer = new SecurityAnalyzer(repo);
    }

    private ApiEndpoint endpoint(String path, AuthType auth) {
        ApiEndpoint e = new ApiEndpoint();
        e.setId(1L);
        e.setMethod("GET");
        e.setService("api.example.com");
        e.setPath(path);
        e.setAuthType(auth);
        return e;
    }

    private List<String> categories() {
        ArgumentCaptor<SecurityFinding> c = ArgumentCaptor.forClass(SecurityFinding.class);
        Mockito.verify(repo, Mockito.atLeast(0)).save(c.capture());
        return c.getAllValues().stream().map(SecurityFinding::getCategory).toList();
    }

    @Test
    void flagsAnonymousObjectAccessAndSensitiveData() {
        analyzer.analyze(endpoint("/api/users/{id}", AuthType.NONE), "https", Map.of(), Map.of(), Map.of(),
                "{\"password\":\"x\"}", 200);
        List<String> cats = categories();
        assertTrue(cats.contains(SecurityAnalyzer.MISSING_AUTH));
        assertTrue(cats.contains(SecurityAnalyzer.BROKEN_ACCESS));
        assertTrue(cats.contains(SecurityAnalyzer.SENSITIVE_DATA));
    }

    @Test
    void flagsTokenInUrlAndWeakHeaders() {
        analyzer.analyze(endpoint("/api/items", AuthType.JWT), "https", Map.of("access_token", "abc"), Map.of(),
                Map.of("server", "nginx/1.18.0"), "{}", 200);
        List<String> cats = categories();
        assertTrue(cats.contains(SecurityAnalyzer.TOKEN_LEAKAGE));
        assertTrue(cats.contains(SecurityAnalyzer.WEAK_HEADERS));
        assertFalse(cats.contains(SecurityAnalyzer.MISSING_AUTH));
    }

    @Test
    void publicPathsAreNotFlaggedForMissingAuth() {
        analyzer.analyze(endpoint("/api/health", AuthType.NONE), "https", Map.of(), Map.of(), Map.of(), "{}", 200);
        assertEquals(0, categories().stream().filter(SecurityAnalyzer.MISSING_AUTH::equals).count());
    }
}
