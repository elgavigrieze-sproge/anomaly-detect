package com.anomalydetect.property;

import com.anomalydetect.buffer.DataBuffer;
import com.anomalydetect.model.TickData;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature: anomaly-detect, Property 4: Buffer FIFO Ordering
 *
 * For any sequence of ticks enqueued into the Data_Buffer, the order in which ticks
 * are dequeued should match the order in which they were enqueued (first-in, first-out).
 *
 * Validates: Requirements 2.4
 */
class BufferFifoPropertyTest {

    @Provide
    Arbitrary<List<TickData>> tickListArbitrary() {
        Arbitrary<TickData> tick = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(6),
                Arbitraries.doubles().between(0.01, 100_000.0),
                Arbitraries.doubles().between(0.0, 10_000_000.0),
                Arbitraries.doubles().between(-1000.0, 1000.0),
                Arbitraries.longs().between(0L, 2_000_000_000L).map(Instant::ofEpochSecond)
        ).as(TickData::new);
        return tick.list().ofMinSize(1).ofMaxSize(50);
    }

    /**
     * Property 4: Buffer FIFO Ordering
     *
     * Given any sequence of ticks enqueued into the buffer (without overflow),
     * the dequeue order should exactly match the enqueue order.
     *
     * Validates: Requirements 2.4
     */
    @Property(tries = 100)
    void dequeueOrderMatchesEnqueueOrder(
            @ForAll("tickListArbitrary") List<TickData> ticks
    ) throws InterruptedException {
        // Use a capacity large enough to hold all ticks (no overflow)
        DataBuffer buffer = new DataBuffer(ticks.size() + 10);

        // Enqueue all ticks
        for (TickData tick : ticks) {
            buffer.offer(tick);
        }

        // Dequeue and verify FIFO order
        List<TickData> dequeued = new ArrayList<>();
        for (int i = 0; i < ticks.size(); i++) {
            dequeued.add(buffer.take());
        }

        assert dequeued.equals(ticks) :
                "Dequeue order should match enqueue order (FIFO)";
    }
}
