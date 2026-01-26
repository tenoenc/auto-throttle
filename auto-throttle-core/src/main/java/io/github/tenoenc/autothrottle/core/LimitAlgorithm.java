package io.github.tenoenc.autothrottle.core;

/**
 * A strategy interface for calculating the optimal concurrency limit.
 * <p>
 * Implementations should analyze the provided {@link Snapshot} (which contains
 * statistics like RTT and throughput) and return a new limit value.
 * </p>
 */
@FunctionalInterface
public interface LimitAlgorithm {

    /**
     * Calculates the next concurrency limit based on the system's performance snapshot.
     *
     * @param currentLimit The current concurrency limit.
     * @param snapshot     The statistics collected during the last window (e.g., avg RTT, min RTT).
     * @return The updated concurrency limit. Must be at least 1.
     */
    int update(int currentLimit, Snapshot snapshot);
}