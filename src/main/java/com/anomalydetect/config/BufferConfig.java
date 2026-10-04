package com.anomalydetect.config;

import com.anomalydetect.model.TickData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Configuration for the tick data buffer.
 */
@Configuration
public class BufferConfig {

    @Bean
    public LinkedBlockingQueue<TickData> tickDataQueue(AnomalyProperties properties) {
        int capacity = properties.getBuffer().getCapacity();
        return new LinkedBlockingQueue<>(capacity);
    }
}
