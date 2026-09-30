package com.apiatlas.interceptor;

import com.apiatlas.config.AtlasProperties;
import com.apiatlas.dto.AnalysisRequest;
import com.apiatlas.dto.EndpointDiscoveredEvent;
import com.apiatlas.model.ApiEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Kafka. After a failure the publisher backs off for a minute so that an
 * unavailable broker never slows down traffic ingestion.
 */
@Component
@Slf4j
public class TrafficPublisher {
    private static final long BACKOFF_MS = 60_000;

    private final KafkaTemplate<String, Object> kafka;
    private final AtlasProperties props;
    private volatile long disabledUntil;

    public TrafficPublisher(KafkaTemplate<String, Object> kafka, AtlasProperties props) {
        this.kafka = kafka;
        this.props = props;
    }

    public boolean endpointDiscovered(ApiEndpoint e) {
        return send(props.kafka().topics().endpointDiscovered(), String.valueOf(e.getId()),
                new EndpointDiscoveredEvent(e.getId(), e.getMethod(), e.getUrl(), e.getService()));
    }

    public boolean analysisRequested(Long sessionId) {
        return send(props.kafka().topics().analysisRequested(), String.valueOf(sessionId), new AnalysisRequest(sessionId));
    }

    private boolean send(String topic, String key, Object payload) {
        if (System.currentTimeMillis() < disabledUntil) {
            return false;
        }
        try {
            kafka.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    pause(ex);
                }
            });
            return true;
        } catch (Exception ex) {
            pause(ex);
            return false;
        }
    }

    private void pause(Throwable ex) {
        disabledUntil = System.currentTimeMillis() + BACKOFF_MS;
        log.warn("Kafka unavailable, event publishing paused for {}s: {}", BACKOFF_MS / 1000, ex.getMessage());
    }
}
