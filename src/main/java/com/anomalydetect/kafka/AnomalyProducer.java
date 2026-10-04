package com.anomalydetect.kafka;

import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer that publishes detected anomalies to the "anomalies" topic.
 * 
 * Downstream consumers (e.g., notification services, dashboards) can subscribe
 * to this topic to receive real-time alerts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyProducer {

    private final KafkaTemplate<String, Anomaly> kafkaTemplate;

    @Value("${anomaly.kafka.topics.anomalies:anomalies}")
    private String anomaliesTopic;

    /**
     * Publishes an anomaly to the Kafka topic.
     * Uses the ticker as the message key for partitioning (all anomalies for 
     * the same ticker go to the same partition, preserving order).
     *
     * @param anomaly the detected anomaly to publish
     */
    public void publish(Anomaly anomaly) {
        String key = anomaly.getTicker();
        
        CompletableFuture<SendResult<String, Anomaly>> future = 
                kafkaTemplate.send(anomaliesTopic, key, anomaly);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish anomaly for {}: {}", 
                        anomaly.getTicker(), ex.getMessage());
            } else {
                log.debug("Published anomaly for {} to partition {} offset {}", 
                        anomaly.getTicker(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
