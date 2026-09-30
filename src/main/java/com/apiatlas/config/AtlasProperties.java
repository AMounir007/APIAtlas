package com.apiatlas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Strongly typed binding of the {@code apiatlas.*} configuration tree.
 */
@ConfigurationProperties(prefix = "apiatlas")
public record AtlasProperties(
        Security security,
        Kafka kafka,
        Discovery discovery,
        Ai ai,
        Analysis analysis) {

    public record Security(Jwt jwt, Cors cors) {
        public record Jwt(String secret, Duration expiration, String issuer) {}
        public record Cors(List<String> allowedOrigins) {}
    }

    public record Kafka(Topics topics) {
        public record Topics(String capturedTraffic, String endpointDiscovered, String analysisRequested) {}
    }

    public record Discovery(int maxConcurrentSessions, Web web, Mobile mobile, Proxy proxy, Capture capture) {
        public record Web(String defaultBrowser, boolean headless, int maxDepth, int maxPages,
                          Duration navigationTimeout, boolean sameOriginOnly) {}
        public record Mobile(String appiumUrl, int maxActions) {}
        public record Proxy(String host, int port, String ingestToken) {}
        public record Capture(int maxBodySize, List<String> excludedExtensions, List<String> maskedHeaders) {}
    }

    public record Ai(boolean enabled, String provider, String baseUrl, String apiKey, String model, Duration timeout) {}

    public record Analysis(Duration unusedThreshold, double duplicateSimilarity) {}
}
