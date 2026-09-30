package com.apiatlas.controller;

import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.CapturedTraffic;
import com.apiatlas.service.TrafficIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Ingest endpoint for external collectors (mitmproxy addon). Authenticated with a shared ingest token. */
@RestController
@RequestMapping("/api/traffic")
@RequiredArgsConstructor
public class TrafficController {
    private static final int MAX_BATCH = 500;

    private final TrafficIngestionService ingestion;
    private final AtlasProperties props;

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingest(@RequestHeader(value = "X-Ingest-Token", required = false) String token,
                                                      @RequestBody List<CapturedTraffic> batch) {
        String expected = props.discovery().proxy().ingestToken();
        if (token == null || expected == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid ingest token"));
        }
        if (batch.size() > MAX_BATCH) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "Batch too large (max " + MAX_BATCH + ")"));
        }
        long accepted = batch.stream().map(ingestion::ingest).filter(java.util.Optional::isPresent).count();
        return ResponseEntity.ok(Map.of("received", batch.size(), "accepted", accepted));
    }
}
