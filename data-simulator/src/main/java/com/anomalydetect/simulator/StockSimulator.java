package com.anomalydetect.simulator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simulates realistic stock market tick data and publishes to Kafka.
 * 
 * Generates normal price movements most of the time, with occasional
 * anomalies (spikes/drops) to trigger the anomaly detector.
 * 
 * Only active when finnhub.enabled=false (fallback mode).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "finnhub.enabled", havingValue = "false", matchIfMissing = true)
public class StockSimulator {

    private final KafkaTemplate<String, TickData> kafkaTemplate;
    private final String topic;
    private final Random random = new Random();

    // Stock configurations with realistic base prices and volatility
    private final List<StockConfig> stocks = List.of(
            new StockConfig("AAPL", 178.50, 0.002, 50_000_000),
            new StockConfig("GOOGL", 141.20, 0.0018, 25_000_000),
            new StockConfig("NVDA", 875.30, 0.003, 40_000_000),
            new StockConfig("MSFT", 378.90, 0.0015, 30_000_000),
            new StockConfig("TSLA", 248.75, 0.004, 80_000_000),
            new StockConfig("AMZN", 178.25, 0.002, 45_000_000),
            new StockConfig("META", 505.60, 0.0025, 20_000_000),
            new StockConfig("BTC/USD", 67500.00, 0.005, 5_000_000),
            new StockConfig("ETH/USD", 3450.00, 0.006, 3_000_000)
    );

    // Track current prices for each stock
    private final Map<String, Double> currentPrices = new HashMap<>();

    // Counter for triggering anomalies
    private int tickCount = 0;

    public StockSimulator(
            KafkaTemplate<String, TickData> kafkaTemplate,
            @Value("${simulator.kafka.topic:stock-ticks}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;

        // Initialize prices
        stocks.forEach(s -> currentPrices.put(s.ticker(), s.basePrice()));
        
        log.info("Stock Simulator initialized with {} tickers", stocks.size());
    }

    /**
     * Generates and publishes tick data every 500ms.
     * ~2 ticks per second per stock = high throughput demo.
     */
    @Scheduled(fixedRate = 500)
    public void generateTicks() {
        tickCount++;

        for (StockConfig stock : stocks) {
            TickData tick = generateTick(stock);
            
            kafkaTemplate.send(topic, stock.ticker(), tick)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send tick for {}: {}", stock.ticker(), ex.getMessage());
                        }
                    });

            if (tickCount % 20 == 0) { // Log every 10 seconds
                log.info("📤 {} @ ${} (Δ{}{:.2f}%)", 
                        tick.ticker(), 
                        String.format("%.2f", tick.price()),
                        tick.priceChange() >= 0 ? "+" : "",
                        tick.priceChange() * 100);
            }
        }
    }

    private TickData generateTick(StockConfig stock) {
        double currentPrice = currentPrices.get(stock.ticker());
        double priceChange;
        double volume;

        // Occasionally generate anomalies (every ~50 ticks per stock, ~25 seconds)
        boolean isAnomaly = random.nextInt(50) == 0;

        if (isAnomaly) {
            // Generate anomalous movement (3-8% spike or drop)
            double anomalyMagnitude = 0.03 + random.nextDouble() * 0.05;
            priceChange = random.nextBoolean() ? anomalyMagnitude : -anomalyMagnitude;
            
            // Anomalies often come with volume spikes
            volume = stock.baseVolume() * (2.0 + random.nextDouble() * 3.0);
            
            log.warn("⚡ Generating ANOMALY for {}: {}{:.2f}% price change", 
                    stock.ticker(), 
                    priceChange >= 0 ? "+" : "",
                    priceChange * 100);
        } else {
            // Normal random walk with stock-specific volatility
            priceChange = (random.nextGaussian() * stock.volatility());
            volume = stock.baseVolume() * (0.5 + random.nextDouble());
        }

        // Apply price change
        double newPrice = currentPrice * (1 + priceChange);
        
        // Prevent negative prices
        newPrice = Math.max(newPrice, currentPrice * 0.5);
        
        currentPrices.put(stock.ticker(), newPrice);

        return new TickData(
                stock.ticker(),
                newPrice,
                volume,
                priceChange,
                Instant.now()
        );
    }

    /**
     * Stock configuration record.
     */
    record StockConfig(
            String ticker,
            double basePrice,
            double volatility,  // Standard deviation of price changes
            double baseVolume   // Average daily volume
    ) {}
}
