package com.apiatlas.utility;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Masks secrets before anything is persisted or logged. */
public final class Masker {
    public static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token", "access_token", "refresh_token", "id_token", "api_key", "apikey", "password", "passwd",
            "pwd", "secret", "client_secret", "jwt", "auth", "session", "sessionid", "authorization");

    private static final Pattern JSON_SECRET = Pattern.compile(
            "(\"(?:password|passwd|pwd|secret|token|access_token|refresh_token|id_token|api_key|apikey|authorization|client_secret)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FORM_SECRET = Pattern.compile(
            "((?:^|&)(?:password|passwd|pwd|secret|token|access_token|refresh_token|client_secret|api_key)=)[^&]*",
            Pattern.CASE_INSENSITIVE);

    private Masker() {}

    public static Map<String, String> maskHeaders(Map<String, String> headers, List<String> masked) {
        Map<String, String> out = new LinkedHashMap<>();
        if (headers == null) return out;
        Set<String> maskedLower = new java.util.HashSet<>();
        if (masked != null) masked.forEach(h -> maskedLower.add(h.toLowerCase(Locale.ROOT)));
        headers.forEach((k, v) -> out.put(k, maskedLower.contains(k.toLowerCase(Locale.ROOT)) ? MASK : v));
        return out;
    }

    public static Map<String, String> maskParams(Map<String, String> params) {
        Map<String, String> out = new LinkedHashMap<>();
        if (params == null) return out;
        params.forEach((k, v) -> out.put(k, isSensitiveKey(k) ? MASK : v));
        return out;
    }

    public static boolean isSensitiveKey(String key) {
        return key != null && SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    public static String maskBody(String body) {
        if (body == null || body.isEmpty()) return body;
        String masked = JSON_SECRET.matcher(body).replaceAll("$1\"" + MASK + "\"");
        return FORM_SECRET.matcher(masked).replaceAll("$1" + MASK);
    }

    public static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max);
    }
}
