package com.apiatlas.utility;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizes raw URL paths into catalog paths (dynamic segments become {@code {id}}, {@code {uuid}} ...). */
public final class PathNormalizer {
    private static final Pattern NUMERIC = Pattern.compile("\\d+");
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HEX = Pattern.compile("[0-9a-fA-F]{16,}");
    private static final Pattern TOKEN = Pattern.compile("(?=.*\\d)(?=.*[A-Za-z])[A-Za-z0-9_-]{24,}");
    private static final Pattern VERSION = Pattern.compile("(?i)(?:^|/)v(\\d+(?:\\.\\d+)?)(?=/|$)");
    private static final Pattern VERSION_SEGMENT = Pattern.compile("(?i)v\\d+(\\.\\d+)?");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+}");

    private PathNormalizer() {}

    public static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        Map<String, Integer> seen = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/")) {
            if (seg.isEmpty()) {
                continue;
            }
            String name = placeholder(seg);
            if (name != null) {
                int n = seen.merge(name, 1, Integer::sum);
                seg = "{" + name + (n > 1 ? n : "") + "}";
            }
            sb.append('/').append(seg);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private static String placeholder(String seg) {
        if (NUMERIC.matcher(seg).matches()) return "id";
        if (UUID.matcher(seg).matches()) return "uuid";
        if (HEX.matcher(seg).matches()) return "hash";
        if (TOKEN.matcher(seg).matches()) return "token";
        return null;
    }

    /** Returns e.g. {@code v2} when the path contains a version segment, otherwise {@code null}. */
    public static String version(String path) {
        Matcher m = VERSION.matcher(path == null ? "" : path);
        return m.find() ? "v" + m.group(1) : null;
    }

    public static String stripVersion(String path) {
        if (path == null) return "/";
        String stripped = VERSION.matcher(path).replaceAll("");
        return stripped.isEmpty() ? "/" : stripped;
    }

    /** First meaningful path segment (skips {@code api}, versions and placeholders). */
    public static String module(String path) {
        if (path == null) return "root";
        for (String seg : path.split("/")) {
            if (seg.isEmpty() || PLACEHOLDER.matcher(seg).matches() || VERSION_SEGMENT.matcher(seg).matches()) continue;
            String s = seg.toLowerCase(Locale.ROOT);
            if (s.equals("api") || s.equals("rest") || s.equals("services") || s.equals("public")) continue;
            return s.length() > 100 ? s.substring(0, 100) : s;
        }
        return "root";
    }

    /** Derives the base URL (scheme://host[:port]) from a stored endpoint url and its path. */
    public static String baseUrl(String url, String path) {
        if (url != null && path != null && url.endsWith(path)) {
            return url.substring(0, url.length() - path.length());
        }
        return url == null ? "" : url;
    }
}
