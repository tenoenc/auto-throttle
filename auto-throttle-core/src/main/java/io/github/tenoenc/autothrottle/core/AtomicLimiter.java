package io.github.tenoenc.autothrottle.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A concurrency limiter that dynamically adjusts the limit based on RTT measurements.
 * <p>
 * This class acts as a gatekeeper, allowing or rejecting requests based on the current
 * concurrency limit. It uses a {@link LimitAlgorithm} (e.g., TCP Vegas) to calculate
 * the optimal limit by analyzing the Round-Trip Time (RTT) of recent requests.
 * </p>
 * <p>
 * Ideally, this class is designed to be lock-free for the hot path (acquire/release)
 * to minimize overhead in high-throughput scenarios.
 * </p>
 */
public class AtomicLimiter {

    private final LimitAlgorithm algorithm;
    private final AtomicRingBuffer ringBuffer;
    private final Snapshot snapshot;
    private final NanoClock clock;

    /**
     * The current concurrency limit.
     * Marked as volatile to ensure visibility across threads without locking.
     */
    private volatile int limit;

    /**
     * The number of requests currently being processed (in-flight).
     */
    private final AtomicInteger currentInflight = new AtomicInteger(0);

    /**
     * The timestamp of the last limit update.
     * Used to enforce the update window interval.
     */
    private final AtomicLong lastUpdateTime;

    /**
     * A spinlock flag to ensure only one thread updates the limit at a time.
     * 0 = unlocked, 1 = locked.
     */
    private final AtomicInteger updateLock = new AtomicInteger(0);

    /**
     * Cursor tracking the last read position in the ring buffer.
     */
    private long lastCursor = 0;

    /**
     * The interval at which the limit is recalculated.
     * Default: 100ms (in nanoseconds).
     */
    private static final long WINDOW_INTERVAL_NS = 100_000_000;

    public AtomicLimiter(LimitAlgorithm algorithm, NanoClock clock) {
        this.algorithm = algorithm;
        this.clock = clock;
        this.ringBuffer = new AtomicRingBuffer(4096);
        this.snapshot = new Snapshot();

        // Initial limit; ideally set conservatively to allow for slow-start.
        this.limit = 20;
        this.lastUpdateTime = new AtomicLong(clock.nanoTime());
    }

    /**
     * Attempts to acquire a permit to proceed with a request.
     *
     * @return {@code true} if the request is allowed (within limit);
     * {@code false} if the request is rejected (limit exceeded).
     */
    public boolean acquire() {
        while (true) {
            int current = currentInflight.get();

            // 1. Fast Failure: Check if the limit is exceeded.
            if (current >= limit) {
                return false;
            }

            // 2. CAS (Compare-And-Swap): Safely increment the in-flight counter.
            // If strictly FIFO is not required, a simple CAS loop is efficient.
            if (currentInflight.compareAndSet(current, current + 1)) {
                return true;
            }
            // CAS failed (contention); retry immediately.
        }
    }

    /**
     * Releases a permit and records the execution metrics.
     * This method also triggers the limit update process (Piggybacking).
     *
     * @param startNanos The timestamp when the permit was acquired.
     */
    public void release(long startNanos) {
        // 1. Decrement the in-flight counter.
        currentInflight.decrementAndGet();

        long now = clock.nanoTime();
        long duration = now - startNanos;

        // 2. Record RTT (Zero-Allocation).
        ringBuffer.add(duration);

        // 3. Try to update the limit (Piggybacking strategy).
        // Instead of a background thread, the worker thread performs the update.
        tryUpdateLimit(now);
    }

    /**
     * Checks if the window has elapsed and updates the limit if necessary.
     * Uses a non-blocking try-lock pattern.
     *
     * @param now The current timestamp.
     */
    private void tryUpdateLimit(long now) {
        long lastUpdate = lastUpdateTime.get();

        // Fast return if the window interval has not yet passed.
        if (now - lastUpdate < WINDOW_INTERVAL_NS) {
            return;
        }

        // Try to acquire the update lock (CAS).
        if (updateLock.compareAndSet(0, 1)) {
            try {
                // Double-check locking: verify timestamp again in case another thread just updated it.
                if (now - lastUpdateTime.get() < WINDOW_INTERVAL_NS) {
                    return;
                }

                // 4. Collect metrics and calculate the new limit.
                lastCursor = ringBuffer.collect(snapshot, lastCursor);
                int newLimit = algorithm.update(limit, snapshot);

                // 5. Apply the new limit (volatile write).
                this.limit = newLimit;

                // Update the last update timestamp.
                lastUpdateTime.set(now);
            } finally {
                // Release the lock.
                updateLock.set(0);
            }
        }
    }

    // Getters for monitoring and testing
    public int getLimit() { return limit; }
    public int getInflight() { return currentInflight.get(); }
}