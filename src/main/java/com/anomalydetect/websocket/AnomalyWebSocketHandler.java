package com.anomalydetect.websocket;

import com.anomalydetect.model.AnomalyAlert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * WebSocket handler that streams anomaly alerts to connected clients in real-time.
 * 
 * Clients connect to /ws/anomalies and receive JSON messages for each detected anomaly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;
    
    // Multicast sink for broadcasting anomalies to all connected clients
    private final Sinks.Many<AnomalyAlert> anomalySink = 
            Sinks.many().multicast().onBackpressureBuffer(1000);

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("WebSocket client connected: {}", sessionId);

        Flux<WebSocketMessage> outbound = anomalySink.asFlux()
                .map(alert -> {
                    try {
                        String json = objectMapper.writeValueAsString(alert);
                        return session.textMessage(json);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize anomaly alert", e);
                        return session.textMessage("{}");
                    }
                })
                .doOnCancel(() -> log.info("WebSocket client disconnected: {}", sessionId))
                .doOnTerminate(() -> log.info("WebSocket stream terminated for: {}", sessionId));

        return session.send(outbound)
                .doOnError(e -> log.error("WebSocket error for {}: {}", sessionId, e.getMessage()));
    }

    /**
     * Broadcasts an anomaly alert to all connected WebSocket clients.
     * Called by the anomaly processing pipeline when a new anomaly is detected.
     *
     * @param alert the anomaly alert to broadcast
     */
    public void broadcast(AnomalyAlert alert) {
        Sinks.EmitResult result = anomalySink.tryEmitNext(alert);
        if (result.isFailure()) {
            log.warn("Failed to broadcast anomaly alert: {}", result);
        } else {
            log.debug("Broadcasted anomaly alert for {}", alert.ticker());
        }
    }

    /**
     * Returns the number of subscribers (for monitoring).
     */
    public int getSubscriberCount() {
        return anomalySink.currentSubscriberCount();
    }
}
