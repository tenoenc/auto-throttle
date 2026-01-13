package io.github.tenoenc.autothrottle.core;

/**
 * A container for statistical data collected over a specific time window.
 * <p>
 * <strong>Design Note:</strong> To minimize Garbage Collection (GC) overhead
 * in high-throughput scenarios, this object is designed to be <b>reused</b>.
 * Instead of creating a new instance for every window, the consumer should
 * call {@link #reset()} and repopulate the data.
 * </p>
 */
public class Snapshot {

    /**
     * The minimum number of samples required to consider the statistics reliable.
     * Prevents the algorithm from reacting to noise or outliers in low-traffic situations.
     */
    private static final int MIN_SAMPLES = 5;

    // Public fields are used intentionally to eliminate Getter/Setter overhead in hot paths.
    public long totalCount;
    public long totalSum;
    public long min;
    public long max;

    public Snapshot() {
        reset();
    }

    /**
     * Resets all statistical data to their initial states.
     * Should be called before reusing this object for a new window.
     */
    public void reset() {
        this.totalCount = 0;
        this.totalSum = 0;
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
    }

    /**
     * Adds a new value to the snapshot and updates statistics.
     *
     * @param value The value to add.
     */
    public void add(long value) {
        this.totalCount++;
        this.totalSum += value;
        if (value < this.min) this.min = value;
        if (value > this.max) this.max = value;
    }

    /**
     * Determines if this snapshot contains enough data to be statistically significant.
     *
     * @return {@code true} if the sample count meets or exceeds the minimum threshold;
     * {@code false} otherwise (noise).
     */
    public boolean isReliable() {
        return totalCount >= MIN_SAMPLES;
    }

    @Override
    public String toString() {
        return "Snapshot{cnt=" + totalCount + ", avg=" + getAverage() + ", valid=" + isReliable() + "}";
    }

    /**
     * Calculates the arithmetic mean.
     *
     * @return The average value, or 0.0 if no samples exist.
     */
    public double getAverage() {
        return totalCount == 0 ? 0 : (double) totalSum / totalCount;
    }
}