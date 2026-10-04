package com.anomalydetect.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaZKBroker;
import redis.embedded.RedisServer;

/**
 * Embedded infrastructure for local development without Docker.
 * Activated with: mvn spring-boot:run -Dspring.profiles.active=local
 * 
 * Provides:
 * - Embedded Kafka broker (in-memory)
 * - Embedded Redis server
 * - H2 database (configured in application-local.yml)
 */
@Configuration
@Profile("local")
public class EmbeddedInfrastructureConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedInfrastructureConfig.class);

    private RedisServer redisServer;
    private EmbeddedKafkaZKBroker kafkaBroker;

    @PostConstruct
    public void startInfrastructure() {
        startKafka();
        startRedis();
    }

    private void startKafka() {
        try {
            log.info("🚀 Starting embedded Kafka broker...");
            kafkaBroker = new EmbeddedKafkaZKBroker(1, true, "stock-ticks", "anomalies");
            kafkaBroker.kafkaPorts(9092);
            kafkaBroker.afterPropertiesSet();
            log.info("✅ Embedded Kafka running on localhost:9092");
        } catch (Exception e) {
            log.error("❌ Failed to start embedded Kafka: {}", e.getMessage());
            throw new RuntimeException("Cannot start embedded Kafka", e);
        }
    }

    private void startRedis() {
        try {
            log.info("🚀 Starting embedded Redis server...");
            redisServer = RedisServer.builder()
                    .port(6379)
                    .setting("maxmemory 64mb")
                    .build();
            redisServer.start();
            log.info("✅ Embedded Redis running on localhost:6379");
        } catch (Exception e) {
            log.warn("⚠️ Could not start embedded Redis: {}. Continuing without Redis caching.", e.getMessage());
        }
    }

    @PreDestroy
    public void stopInfrastructure() {
        if (kafkaBroker != null) {
            log.info("🛑 Stopping embedded Kafka...");
            kafkaBroker.destroy();
        }
        if (redisServer != null && redisServer.isActive()) {
            log.info("🛑 Stopping embedded Redis...");
            redisServer.stop();
        }
    }

    @Bean
    public NewTopic stockTicksTopic() {
        return new NewTopic("stock-ticks", 1, (short) 1);
    }

    @Bean
    public NewTopic anomaliesTopic() {
        return new NewTopic("anomalies", 1, (short) 1);
    }
}
