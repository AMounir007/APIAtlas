package com.apiatlas.interceptor;

import com.apiatlas.ai.AiAnalysisService;
import com.apiatlas.dto.AnalysisRequest;
import com.apiatlas.dto.CapturedTraffic;
import com.apiatlas.service.TrafficIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Kafka consumers: external collectors publish captured traffic; analysis jobs trigger AI enrichment. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrafficListener {
    private final TrafficIngestionService ingestion;
    private final AiAnalysisService ai;

    @KafkaListener(topics = "${apiatlas.kafka.topics.captured-traffic}", groupId = "api-atlas-ingest")
    public void onTraffic(CapturedTraffic traffic) {
        ingestion.ingest(traffic);
    }

    @KafkaListener(topics = "${apiatlas.kafka.topics.analysis-requested}", groupId = "api-atlas-analysis",
            properties = {"spring.json.value.default.type=com.apiatlas.dto.AnalysisRequest"})
    public void onAnalysisRequested(AnalysisRequest request) {
        log.info("Analysis requested (session={})", request.sessionId());
        ai.enrichPending();
    }
}
