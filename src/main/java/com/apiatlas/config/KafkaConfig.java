package com.apiatlas.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    NewTopic capturedTrafficTopic(AtlasProperties p) {
        return TopicBuilder.name(p.kafka().topics().capturedTraffic()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic endpointDiscoveredTopic(AtlasProperties p) {
        return TopicBuilder.name(p.kafka().topics().endpointDiscovered()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic analysisRequestedTopic(AtlasProperties p) {
        return TopicBuilder.name(p.kafka().topics().analysisRequested()).partitions(1).replicas(1).build();
    }
}
