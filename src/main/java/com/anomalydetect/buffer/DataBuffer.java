package com.anomalydetect.buffer;

import com.anomalydetect.config.AnomalyProperties;
import com.anomalydetect.model.TickData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe bounded buffer that decouples ingestion from processing.
 * Uses a {@link LinkedBlockingQueue} with configurable capacity.
 * When full, drops the oldest tick and logs a warning before enqueuing the new one.
 */
@Component
public class DataBuffer {

    private static final Logger log = LoggerFactory.getLogger(DataBuffer.class);

    private final LinkedBlockingQueue<TickData> queue;

    public DataBuffer(AnomalyProperties properties) {
        this(properties.getBuffer().getCapacity());
    }

    /**
     * Creates a DataBuffer with the specified capacity.
     *
     * @param capacity maximum number of ticks the buffer can hold
     */
    public DataBuffer(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Enqueues a tick. If the buffer is full, drops the oldest tick and re-offers.
     *
     * @param tick the tick data to enqueue
     * @return true if the tick was successfully enqueued
     */
    public boolean offer(TickData tick) {
        if (queue.offer(tick)) {
            return true;
        }
        // Queue is full — drop oldest
        TickData dropped = queue.poll();
        if (dropped != null) {
            log.warn("Buffer full — dropped oldest tick: ticker={}, timestamp={}, price={}",
                    dropped.ticker(), dropped.timestamp(), dropped.price());
        }
        return queue.offer(tick);
    }

    /**
     * Blocking take for consumers. Waits until a tick is available.
     *
     * @return the next tick in FIFO order
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public TickData take() throws InterruptedException {
        return queue.take();
    }

    /**
     * Returns the current number of ticks in the buffer (for health monitoring).
     */
    public int depth() {
        return queue.size();
    }
}
