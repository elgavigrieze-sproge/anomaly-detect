package com.anomalydetect.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client that connects to Finnhub's real-time trade feed
 * and publishes tick data to Kafka.
 * 
 * Activated when finnhub.enabled=true and finnhub.api-key is set.
 */
@Component
@ConditionalOnProperty(name = "finnhub.enabled", havingValue = "true")
public class FinnhubWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubWebSocketClient.class);
    private static final String FINNHUB_WS_URL = "wss://ws.finnhub.io?token=";

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Value("${finnhub.symbols:AAPL,GOOGL,MSFT,NVDA,AMZN,META,TSLA}")
    private List<String> symbols;

    @Value("${kafka.topic.ticks:stock-ticks}")
    private String ticksTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    
    private WebSocketClient wsClient;
    private ScheduledExecutorService reconnectScheduler;
    private volatile boolean running = true;

    public FinnhubWebSocketClient(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        log.info("Finnhub client enabled. Connecting to real-time feed for symbols: {}", symbols);
        establishConnection();
    }

    @PreDestroy
    public void disconnect() {
        running = false;
        if (reconnectScheduler != null) {
            reconnectScheduler.shutdown();
        }
        if (wsClient != null) {
            wsClient.close();
        }
        log.info("Finnhub WebSocket client disconnected");
    }

    private void establishConnection() {
        try {
            URI serverUri = new URI(FINNHUB_WS_URL + apiKey);
            
            wsClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Connected to Finnhub WebSocket");
                    subscribeToSymbols();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Finnhub WebSocket closed: {} (code: {}). Remote: {}", reason, code, remote);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Finnhub WebSocket error: {}", ex.getMessage());
                }
            };

            wsClient.connect();
        } catch (Exception e) {
            log.error("Failed to connect to Finnhub: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void subscribeToSymbols() {
        for (String symbol : symbols) {
            try {
                String subscribeMsg = objectMapper.writeValueAsString(
                    Map.of("type", "subscribe", "symbol", symbol)
                );
                wsClient.send(subscribeMsg);
                log.info("Subscribed to {}", symbol);
            } catch (Exception e) {
                log.error("Failed to subscribe to {}: {}", symbol, e.getMessage());
            }
        }
    }

    private void handleMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String type = root.path("type").asText();

            if ("trade".equals(type)) {
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode trade : data) {
                        processTrade(trade);
                    }
                }
            } else if ("ping".equals(type)) {
                // Finnhub sends pings to keep connection alive
                log.debug("Received ping from Finnhub");
            }
        } catch (Exception e) {
            log.error("Error processing Finnhub message: {}", e.getMessage());
        }
    }

    private void processTrade(JsonNode trade) {
        try {
            String symbol = trade.path("s").asText();
            double price = trade.path("p").asDouble();
            long volume = trade.path("v").asLong();
            long timestamp = trade.path("t").asLong(); // Unix timestamp in ms

            // Calculate price change from last known price
            Double lastPrice = lastPrices.get(symbol);
            double priceChange = (lastPrice != null) ? price - lastPrice : 0.0;
            lastPrices.put(symbol, price);

            // Create tick data
            TickData tick = new TickData(
                symbol,
                price,
                volume,
                priceChange,
                Instant.ofEpochMilli(timestamp)
            );

            // Publish to Kafka
            String json = objectMapper.writeValueAsString(tick);
            kafkaTemplate.send(ticksTopic, symbol, json);

            log.debug("Published real trade: {} @ ${} (vol: {}, change: {})",
                    symbol, price, volume, priceChange);

        } catch (Exception e) {
            log.error("Error processing trade: {}", e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (running) {
            log.info("Scheduling reconnect in 5 seconds...");
            reconnectScheduler.schedule(this::establishConnection, 5, TimeUnit.SECONDS);
        }
    }
}
