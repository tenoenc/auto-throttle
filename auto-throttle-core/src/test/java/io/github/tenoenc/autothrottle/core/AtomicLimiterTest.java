package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicLimiterTest {

    @Test
    void should_AdjustLimitDynamically_BasedOnRttChanges() {
        // Mocking time to simulate network conditions deterministically.
        TestClock clock = new TestClock();

        // Using standard Vegas parameters (Alpha=3, Beta=6)
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);
        AtomicLimiter limiter = new AtomicLimiter(algo, clock);

        // [Phase 1: Learning Phase] - Fast Server (2ms RTT)
        // Scenario: Max out the current limit (20) with fast responses.
        // Expectation: Queue size ~ 0 -> Limit should increase.
        for (int i = 0; i < 60; i++) {
            if (limiter.acquire()) {
                clock.forward(2_000_000); // 2ms
                limiter.release(clock.nanoTime() - 2_000_000);
            }
        }

        // Forward time to pass the window interval (100ms)
        clock.forward(150_000_000);

        // Trigger update
        if (limiter.acquire()) limiter.release(clock.nanoTime());

        // Verification: Limit should have increased from initial 20.
        assertTrue(limiter.getLimit() > 20, "Limit should increase when the server is fast.");
        int peakLimit = limiter.getLimit();

        // [Phase 2: Congestion Phase] - Slow Server (Latency spikes to 15ms)
        // Calculation:
        // - Window: 100ms / 15ms per req = ~6.6 samples (Enough to pass noise filter).
        // - Vegas Logic:
        //   - minRTT = 2ms (Learned in Phase 1)
        //   - currentRTT = 15ms
        //   - QueueSize = Limit * (1 - 2/15) = Limit * 0.86
        //   - If Limit is ~20, QueueSize is ~17.
        //   - 17 > Beta(6) -> Limit MUST decrease.
        for (int i = 0; i < 100; i++) {
            if (limiter.acquire()) {
                clock.forward(15_000_000); // 15ms (7.5x slower)
                limiter.release(clock.nanoTime() - 15_000_000);
            }
        }

        // Forward time and trigger update
        clock.forward(150_000_000);
        if (limiter.acquire()) limiter.release(clock.nanoTime());

        // Verification: Limit should decrease to relieve congestion.
        int finalLimit = limiter.getLimit();
        System.out.println("Peak Limit: " + peakLimit + " -> Final Limit: " + finalLimit);
        assertTrue(limiter.getLimit() < peakLimit, "Limit should decrease when latency spikes.");
    }

    // A simple mock implementation of NanoClock for testing.
    static class TestClock implements NanoClock {
        private long now = 0;

        @Override
        public long nanoTime() { return now; }
        public void forward(long nanos) { now += nanos; }
    }
}