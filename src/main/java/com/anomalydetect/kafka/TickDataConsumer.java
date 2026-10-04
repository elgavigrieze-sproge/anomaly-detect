package com.anomalydetect.kafka;

import com.anomalydetect.ai.AnomalyExplainerService;
import com.anomalydetect.buffer.DataBuffer;
import com.anomalydetect.detection.AnomalyDetector;
import com.anomalydetect.model.Anomaly;
import com.anomalydetect.model.AnomalyAlert;
import com.anomalydetect.model.TickData;
import com.anomalydetect.websocket.AnomalyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka consumer that ingests tick data from the "stock-ticks" topic.
 * 
 * Each tick is:
 * 1. Added to the DataBuffer for baseline calculations
 * 2. Evaluated by the AnomalyDetector
 * 3. If anomalous, enriched with AI explanation and broadcast via WebSocket
 * 4. Published to the "anomalies" Kafka topic
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TickDataConsumer {

    private final DataBuffer dataBuffer;
    private final AnomalyDetector anomalyDetector;
    private final AnomalyProducer anomalyProducer;
    private final AnomalyExplainerService explainerService;
    private final AnomalyWebSocketHandler webSocketHandler;

    @KafkaListener(
            topics = "${anomaly.kafka.topics.ticks:stock-ticks}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "tickDataListenerFactory"
    )
    public void onTick(TickData tick) {
        log.debug("Received tick: {} @ ${} (volume: {})", 
                tick.ticker(), tick.price(), tick.volume());

        // 1. Add to buffer for baseline calculations
        dataBuffer.offer(tick);

        // 2. Evaluate for anomalies
        Optional<Anomaly> anomaly = anomalyDetector.evaluate(tick);

        // 3. If anomalous, process and broadcast
        anomaly.ifPresent(this::processAnomaly);
    }

    private void processAnomaly(Anomaly anomaly) {
        log.info("🚨 Anomaly detected: {} {} z={:.2f} @ ${}", 
                anomaly.getTicker(), anomaly.getAnomalyType(), 
                anomaly.getZScore(), anomaly.getPrice());

        // Get AI explanation (async, with fallback)
        String explanation = explainerService.explainSync(anomaly);

        // Create alert with explanation
        AnomalyAlert alert = AnomalyAlert.from(anomaly, explanation);

        // Broadcast to WebSocket clients
        webSocketHandler.broadcast(alert);

        // Publish to Kafka for downstream consumers
        anomalyProducer.publish(anomaly);

        log.info("📢 Alert broadcasted: {} - {}", anomaly.getTicker(), 
                explanation.substring(0, Math.min(80, explanation.length())) + "...");
    }
}
