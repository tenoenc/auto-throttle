package io.github.tenoenc.autothrottle.core;

/**
 * An interface for recording performance metrics (e.g., Round-Trip Time).
 * <p>
 * This registry is designed for critical paths where latency is sensitive.
 * </p>
 */
public interface MetricRegistry {

    /**
     * Records a measured value (e.g., latency in nanoseconds).
     * <p>
     * <strong>Constraint:</strong> Implementations MUST NOT allocate heap memory
     * (e.g., using the {@code new} keyword) during this method call to prevent
     * Garbage Collection pressure.
     * </p>
     *
     * @param value The measured value to record.
     */
    void add(long value);
}