package com.apiatlas.analyzer;

import com.apiatlas.model.ApiEndpoint;
import com.apiatlas.model.Enums.AuthType;
import com.apiatlas.model.Enums.Severity;
import com.apiatlas.model.SecurityFinding;
import com.apiatlas.repository.SecurityFindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Detects OWASP-API-Security style issues on every captured transaction. Findings never contain secret values. */
@Component
@RequiredArgsConstructor
public class SecurityAnalyzer {
    public static final String MISSING_AUTH = "MISSING_AUTHENTICATION";
    public static final String SENSITIVE_DATA = "SENSITIVE_DATA_EXPOSURE";
    public static final String WEAK_HEADERS = "WEAK_SECURITY_HEADERS";
    public static final String TOKEN_LEAKAGE = "TOKEN_LEAKAGE";
    public static final String BROKEN_ACCESS = "BROKEN_ACCESS_CONTROL";
    public static final String INSECURE_TRANSPORT = "INSECURE_TRANSPORT";

    private static final Pattern PUBLIC_PATH = Pattern.compile(
            "(?i)/(login|signin|sign-in|auth|token|oauth|register|signup|health|public|ping|status|config|version)");
    private static final Pattern OBJECT_ID = Pattern.compile("\\{(id|uuid|hash|token)\\d*}");
    private static final Pattern VERSIONED_SERVER = Pattern.compile("\\d+\\.\\d+");

    private static final Map<String, Pattern> SENSITIVE = new LinkedHashMap<>();

    static {
        SENSITIVE.put("password/secret field", Pattern.compile(
                "\"(password|passwd|secret|private_key|client_secret)\"\\s*:\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE));
        SENSITIVE.put("token field", Pattern.compile(
                "\"(access_token|refresh_token|id_token|api_key|apikey)\"\\s*:\\s*\"[^\"]+\"", Pattern.CASE_INSENSITIVE));
        SENSITIVE.put("national id / SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        SENSITIVE.put("payment card number", Pattern.compile(
                "\\b(?:4\\d{3}|5[1-5]\\d{2}|3[47]\\d{2}|6011)[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{3,4}\\b"));
    }

    private final SecurityFindingRepository findings;

    @Transactional
    public void analyze(ApiEndpoint e, String scheme, Map<String, String> query, Map<String, String> reqHeaders,
                        Map<String, String> respHeaders, String body, int status) {
        boolean success = status >= 200 && status < 300;
        boolean publicPath = PUBLIC_PATH.matcher(e.getPath()).find();
        boolean cookieSession = reqHeaders != null && reqHeaders.containsKey("cookie");
        boolean https = "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);

        // API2:2023 Broken Authentication
        if (e.getAuthType() == AuthType.NONE && !cookieSession) {
            if (success && !publicPath) {
                boolean mutating = !List.of("GET", "HEAD").contains(e.getMethod());
                record(e, MISSING_AUTH, "API2:2023", mutating ? Severity.HIGH : Severity.MEDIUM,
                        "Endpoint accessible without authentication",
                        "A successful (" + status + ") response was returned without any credentials.");
            }
        } else {
            resolve(e, MISSING_AUTH);
        }

        // API3:2023 Broken Object Property Level Authorization / excessive data exposure
        if (body != null && !body.isEmpty()) {
            List<String> hits = new ArrayList<>();
            SENSITIVE.forEach((label, p) -> {
                if (p.matcher(body).find() && !(publicPath && label.equals("token field"))) hits.add(label);
            });
            if (!hits.isEmpty()) {
                record(e, SENSITIVE_DATA, "API3:2023", Severity.HIGH, "Sensitive data exposed in response",
                        "Response body contains: " + String.join(", ", hits) + ".");
            }
        }

        // API8:2023 Security Misconfiguration - headers
        if (respHeaders != null && !respHeaders.isEmpty()) {
            List<String> problems = new ArrayList<>();
            if (!respHeaders.containsKey("x-content-type-options")) problems.add("missing X-Content-Type-Options");
            if (https && !respHeaders.containsKey("strict-transport-security")) problems.add("missing Strict-Transport-Security");
            if (e.getAuthType() != AuthType.NONE && !respHeaders.containsKey("cache-control")) problems.add("missing Cache-Control on authenticated response");
            if ("*".equals(respHeaders.get("access-control-allow-origin"))
                    && "true".equalsIgnoreCase(respHeaders.get("access-control-allow-credentials"))) {
                problems.add("CORS wildcard origin with credentials");
            }
            String server = respHeaders.getOrDefault("server", "");
            if (VERSIONED_SERVER.matcher(server).find()) problems.add("server version disclosed");
            if (respHeaders.containsKey("x-powered-by")) problems.add("X-Powered-By disclosed");
            if (!problems.isEmpty()) {
                record(e, WEAK_HEADERS, "API8:2023", Severity.LOW, "Weak security headers",
                        String.join("; ", problems) + ".");
            }
        }

        // Token leakage in URL
        if (query != null) {
            List<String> leaked = query.keySet().stream()
                    .filter(k -> com.apiatlas.utility.Masker.isSensitiveKey(k)).map(k -> k.toLowerCase(Locale.ROOT)).toList();
            if (!leaked.isEmpty()) {
                record(e, TOKEN_LEAKAGE, "API2:2023", Severity.HIGH, "Credentials passed in URL query string",
                        "Query parameters " + leaked + " may end up in logs, browser history and proxies.");
            }
        }

        // Transport security
        if (!https && !"localhost".equalsIgnoreCase(e.getService().split(":")[0])
                && (e.getAuthType() != AuthType.NONE || (query != null && query.keySet().stream().anyMatch(com.apiatlas.utility.Masker::isSensitiveKey)))) {
            record(e, INSECURE_TRANSPORT, "API8:2023", Severity.MEDIUM, "Credentials sent over an unencrypted channel",
                    "The endpoint is reached via " + scheme + ".");
        }

        // API1:2023 Broken Object Level Authorization
        if (OBJECT_ID.matcher(e.getPath()).find() && !"POST".equals(e.getMethod())) {
            if (e.getAuthType() == AuthType.NONE && !cookieSession && success) {
                record(e, BROKEN_ACCESS, "API1:2023", Severity.HIGH, "Object identifier accessible without authentication",
                        "Objects can be enumerated by changing the identifier in the path.");
            } else {
                record(e, BROKEN_ACCESS, "API1:2023", Severity.LOW, "Verify object-level authorization",
                        "Path contains an object identifier; test that other users' objects are not accessible (IDOR).");
            }
        }
    }

    private void record(ApiEndpoint e, String category, String owasp, Severity severity, String title, String description) {
        Optional<SecurityFinding> existing = findings.findByEndpointIdAndCategory(e.getId(), category);
        if (existing.isPresent()) {
            SecurityFinding f = existing.get();
            if (f.isResolved()) {
                f.setResolved(false);
                f.setSeverity(severity);
                f.setTitle(title);
                f.setDescription(description);
                findings.save(f);
            }
            return;
        }
        SecurityFinding f = new SecurityFinding();
        f.setEndpointId(e.getId());
        f.setEndpointLabel(e.getMethod() + " " + e.getService() + e.getPath());
        f.setCategory(category);
        f.setOwasp(owasp);
        f.setSeverity(severity);
        f.setTitle(title);
        f.setDescription(description);
        f.setDetectedAt(Instant.now());
        findings.save(f);
    }

    private void resolve(ApiEndpoint e, String category) {
        findings.findByEndpointIdAndCategory(e.getId(), category).ifPresent(f -> {
            if (!f.isResolved()) {
                f.setResolved(true);
                findings.save(f);
            }
        });
    }
}
