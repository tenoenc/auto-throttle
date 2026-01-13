package io.github.tenoenc.autothrottle.core;

/**
 * A concurrency control algorithm based on TCP Vegas.
 * <p>
 * This algorithm estimates the congestion level of the system by comparing the
 * current Round-Trip Time (RTT) with the minimum observed RTT (physical baseline).
 * It attempts to keep the estimated "queue size" (virtual backlog) within a
 * specific range [alpha, beta].
 * </p>
 */
public class VegasLimitAlgorithm implements LimitAlgorithm {

    /**
     * The lower bound of the target queue size.
     * If the estimated queue size is less than this value, the limit increases
     * to utilize more capacity.
     */
    private final int alpha;

    /**
     * The upper bound of the target queue size.
     * If the estimated queue size exceeds this value, the limit decreases
     * to prevent latency degradation.
     */
    private final int beta;

    /**
     * The minimum RTT observed so far (representing the physical processing time
     * without queuing delay).
     */
    private double minRtt = Double.MAX_VALUE;

    /**
     * Creates a Vegas algorithm with default parameters (alpha=3, beta=6).
     * This range typically provides a good balance between throughput and latency.
     */
    public VegasLimitAlgorithm() {
        this(3, 6);
    }

    /**
     * Creates a Vegas algorithm with custom parameters.
     *
     * @param alpha The lower bound threshold.
     * @param beta  The upper bound threshold.
     */
    public VegasLimitAlgorithm(int alpha, int beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public int update(int currentLimit, Snapshot snapshot) {
        // 1. Noise Filtering: Skip update if the data is statistically insignificant.
        if (!snapshot.isReliable()) {
            return currentLimit;
        }

        double currentAvgRtt = snapshot.getAverage();
        if (currentAvgRtt <= 0) return currentLimit;

        // 2. Learn Minimum RTT (Physical Baseline).
        // Since the app might become faster (e.g., JIT compilation), minRtt tracks the lowest seen value.
        if (currentAvgRtt < minRtt) {
            minRtt = currentAvgRtt;
        }

        // 3. Estimate Queue Size using Little's Law principles.
        // QueueSize = Limit * (1 - minRTT / currentRTT)
        // e.g., If RTT doubles (current = 2 * min), roughly half the requests are queued.
        double queueSize = currentLimit * (1 - minRtt / currentAvgRtt);

        // 4. Adjust Limit based on congestion thresholds.
        int newLimit = currentLimit;

        if (queueSize < alpha) {
            // Under-utilized: The queue is empty or small. Increase limit.
            newLimit = currentLimit + 1;
        } else if (queueSize > beta) {
            // Congested: The queue is growing too large. Decrease limit.
            newLimit = currentLimit - 1;
        }
        // If alpha <= queueSize <= beta, the system is in a stable state. Keep current limit.

        // Safety Guard: Ensure at least one request can be processed.
        return Math.max(1, newLimit);
    }
}