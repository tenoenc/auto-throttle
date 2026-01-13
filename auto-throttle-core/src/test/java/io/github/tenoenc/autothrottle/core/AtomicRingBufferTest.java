package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicRingBufferTest {

    @Test
    void should_CollectMetrics_FromStartingCursor() {
        // given
        int size = 1024;
        AtomicRingBuffer ringBuffer = new AtomicRingBuffer(size);
        Snapshot snapshot = new Snapshot();

        // when: Adding 3 data points
        ringBuffer.add(100);
        ringBuffer.add(200);
        ringBuffer.add(300);

        // then: Reading from cursor 0
        long nextCursor = ringBuffer.collect(snapshot, 0);

        assertEquals(3, nextCursor, "Cursor should move by 3.");
        assertEquals(3, snapshot.totalCount, "Total count should be 3.");
        assertEquals(200.0, snapshot.getAverage(), "Average should be 200.");
        assertEquals(300, snapshot.max, "Max value should be 300.");
    }

    @Test
    void should_ReadOnlyRecentData_When_BufferOverwritesOccur() {
        // given: A very small buffer
        AtomicRingBuffer ringBuffer = new AtomicRingBuffer(4);
        Snapshot snapshot = new Snapshot();

        // when: Adding 5 items (0, 1, 2, 3, 4)
        // The value '0' should be overwritten by '4' in a circular manner.
        for (int i = 0; i < 5; i++) {
            ringBuffer.add(i);
        }

        // then: Attempting to read from the beginning (Cursor 0)
        long nextCursor = ringBuffer.collect(snapshot, 0);

        // The reader should only see the latest 'bufferSize' elements (1, 2, 3, 4).
        assertEquals(5, nextCursor); // Cursor logically moves to 5
        assertEquals(4, snapshot.totalCount, "Should collect only the buffer capacity (4) when overwritten.");
        assertEquals(4, snapshot.max, "Max value should be the latest element.");
        assertEquals(1, snapshot.min, "Min value should start from 1 (0 was overwritten).");
    }
}