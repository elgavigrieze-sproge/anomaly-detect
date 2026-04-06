package com.anomalydetect.property;

import com.anomalydetect.buffer.DataBuffer;
import com.anomalydetect.model.TickData;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature: anomaly-detect, Property 3: Buffer Capacity and Overflow
 *
 * For any buffer with capacity N that is full, offering a new tick should result in
 * the buffer still containing exactly N items, the oldest tick being removed, and
 * the new tick being present in the buffer.
 *
 * Validates: Requirements 2.1, 2.2
 */
class BufferCapacityPropertyTest {

    @Provide
    Arbitrary<TickData> tickDataArbitrary() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(6),
                Arbitraries.doubles().between(0.01, 100_000.0),
                Arbitraries.doubles().between(0.0, 10_000_000.0),
                Arbitraries.doubles().between(-1000.0, 1000.0),
                Arbitraries.longs().between(0L, 2_000_000_000L).map(Instant::ofEpochSecond)
        ).as(TickData::new);
    }

    /**
     * Property 3: Buffer Capacity and Overflow
     *
     * Given a buffer of capacity N that is completely full,
     * when a new tick is offered,
     * then the buffer should still contain exactly N items,
     * the oldest tick should have been removed,
     * and the new tick should be present (retrievable via take).
     *
     * Validates: Requirements 2.1, 2.2
     */
    @Property(tries = 100)
    void fullBufferOverflowMaintainsCapacityAndDropsOldest(
            @ForAll @IntRange(min = 1, max = 200) int capacity,
            @ForAll("tickDataArbitrary") TickData newTick
    ) throws InterruptedException {
        DataBuffer buffer = new DataBuffer(capacity);

        // Fill the buffer to capacity with distinguishable ticks
        List<TickData> filledTicks = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            TickData tick = new TickData("FILL-" + i, i + 1.0, 100.0, 0.0,
                    Instant.ofEpochSecond(1_000_000 + i));
            buffer.offer(tick);
            filledTicks.add(tick);
        }

        // Buffer should be at capacity
        assert buffer.depth() == capacity : "Buffer should be full before overflow test";

        // Offer a new tick — should trigger overflow
        boolean offered = buffer.offer(newTick);

        // After overflow: buffer still has exactly N items
        assert offered : "offer() should return true even on overflow";
        assert buffer.depth() == capacity :
                "Buffer depth should remain " + capacity + " after overflow, but was " + buffer.depth();

        // Drain the buffer and verify: oldest (FILL-0) is gone, newTick is present
        List<TickData> drained = new ArrayList<>();
        while (buffer.depth() > 0) {
            drained.add(buffer.take());
        }

        // The first original tick (FILL-0) should have been dropped
        TickData droppedTick = filledTicks.get(0);
        assert !drained.contains(droppedTick) :
                "Oldest tick should have been dropped but was still in buffer";

        // The new tick should be present
        assert drained.contains(newTick) :
                "Newly offered tick should be present in the buffer";

        // Total drained should equal capacity
        assert drained.size() == capacity :
                "Drained size should equal capacity " + capacity + ", but was " + drained.size();
    }
}
