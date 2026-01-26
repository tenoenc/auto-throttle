package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationTest {

    @Test
    void should_MarkSnapshotAsUnreliable_When_SampleCountIsLow() {
        // given
        Snapshot snapshot = new Snapshot();

        // when: Adding samples fewer than MIN_SAMPLES (5)
        snapshot.add(100);
        snapshot.add(100);
        snapshot.add(100);

        // then: Should be treated as noise (unreliable)
        assertFalse(snapshot.isReliable(), "Snapshot should be unreliable due to insufficient samples.");

        // when: Adding more samples to exceed the threshold
        snapshot.add(100);
        snapshot.add(100);
        snapshot.add(100); // Total 6 samples

        // then: Should be reliable
        assertTrue(snapshot.isReliable(), "Snapshot should be reliable once minimum sample count is met.");
    }

    @Test
    void should_AggregateDataCorrectly_AcrossTimeWindows() {
        // Simulation of logical time boundaries.
        // Verifies that the ring buffer correctly collects data between cursors.

        AtomicRingBuffer buffer = new AtomicRingBuffer(1024);
        Snapshot snapshot = new Snapshot();
        long lastCursor = 0;

        // --- Window 1 (e.g., 0ms ~ 100ms) ---
        // given: Traffic generation
        for (int i = 0; i < 10; i++) buffer.add(10);

        // when: Aggregating stats
        lastCursor = buffer.collect(snapshot, lastCursor);

        // then: Valid stats expected
        assertTrue(snapshot.isReliable());
        assertEquals(10.0, snapshot.getAverage());

        // --- Window 2 (e.g., 100ms ~ 200ms) ---
        // given: Low traffic (Noise)
        buffer.add(50); // Only 1 request

        // when: Aggregating stats again
        lastCursor = buffer.collect(snapshot, lastCursor);

        // then: Should be ignored due to low sample count
        assertEquals(1, snapshot.totalCount);
        assertFalse(snapshot.isReliable(), "Window 2 should be ignored due to low sample count (Noise).");
    }
}