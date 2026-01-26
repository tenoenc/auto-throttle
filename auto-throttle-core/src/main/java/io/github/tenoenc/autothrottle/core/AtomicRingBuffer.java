package io.github.tenoenc.autothrottle.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A high-performance, fixed-size ring buffer for storing metrics.
 * <p>
 * This class uses atomic cursors to allow concurrent writes without heavy locking.
 * It is designed for sampling purposes where slight data loss (overwriting)
 * during extreme contention is acceptable in exchange for performance.
 * </p>
 */
public class AtomicRingBuffer implements MetricRegistry {

    private final long[] buffer;
    private final int mask;
    private final AtomicLong cursor = new AtomicLong(0);

    /**
     * Creates a new ring buffer.
     *
     * @param bufferSize The size of the buffer. Must be a power of two.
     * @throws IllegalArgumentException if bufferSize is not a power of two.
     */
    public AtomicRingBuffer(int bufferSize) {
        if (!isPowerOfTwo(bufferSize)) {
            throw new IllegalArgumentException("Buffer size must be a power of two.");
        }
        this.buffer = new long[bufferSize];
        // Calculate mask: e.g., 1024 (100...00) -> 1023 (011...11)
        this.mask = bufferSize - 1;
    }

    @Override
    public void add(long value) {
        // 1. Atomically increment the cursor to reserve a slot.
        long currentCursor = cursor.getAndIncrement();

        // 2. Calculate index using bitwise AND (faster than modulo).
        // e.g., currentCursor & (size - 1)
        int index = (int) (currentCursor & mask);

        // 3. Write value to the buffer.
        // Note: A race condition may occur here if the reader catches up,
        // or if multiple writers wrap around. For metric sampling, this is acceptable.
        buffer[index] = value;
    }

    /**
     * Collects metrics from the buffer into the provided snapshot.
     * Reads from {@code lastCursor} up to the current head of the buffer.
     *
     * @param targetSnapshot The snapshot object to populate (reused to avoid allocation).
     * @param lastCursor     The cursor position from the previous collection.
     * @return The new cursor position representing the end of the read data.
     */
    public long collect(Snapshot targetSnapshot, long lastCursor) {
        long currentCursor = cursor.get();

        // Nothing new to read.
        if (currentCursor == lastCursor) {
            return currentCursor;
        }

        long countToRead = currentCursor - lastCursor;
        long readStart = lastCursor;

        // If the buffer has wrapped around (overflowed), skip old data.
        // We only read the most recent 'buffer.length' elements.
        if (countToRead > buffer.length) {
            readStart = currentCursor - buffer.length;
        }

        targetSnapshot.reset(); // Reset snapshot state before aggregation.

        // Iterate and aggregate values.
        // Reads are performed without locking; values may change during read, which is acceptable.
        for (long i = readStart; i < currentCursor; i++) {
            int index = (int) (i & mask);
            long value = buffer[index];
            targetSnapshot.add(value);
        }

        return currentCursor;
    }

    private boolean isPowerOfTwo(int number) {
        return number > 0 && (number & (number - 1)) == 0;
    }

    public long[] getBuffer() { return buffer; }
}