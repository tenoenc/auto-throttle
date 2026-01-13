package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VegasLimitAlgorithmTest {

    @Test
    void should_IncreaseLimit_When_QueueSizeIsLow() {
        // given: Current limit 10, Optimal range (Alpha=3 ~ Beta=6)
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);
        Snapshot snapshot = new Snapshot();

        // Scenario: Very fast server response (avg 10ms)
        // Filling data to pass noise filter (min samples > 5)
        for (int i = 0; i < 10; i++) snapshot.add(10);

        // when: Updating limit
        int newLimit = algo.update(10, snapshot);

        // then:
        // - minRTT learned = 10ms
        // - currentRTT = 10ms
        // - queueSize = 10 * (1 - 10/10) = 0
        // - Since 0 < Alpha(3), the limit should increase.
        assertEquals(11, newLimit, "Limit should increase when the queue is empty (fast response).");
    }

    @Test
    void should_DecreaseLimit_When_QueueSizeIsHigh() {
        // given
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);

        // 1. Learn baseline (minRTT = 10ms)
        Snapshot fastSnap = new Snapshot();
        for (int i = 0; i < 10; i++) fastSnap.add(10);
        algo.update(10, fastSnap);

        // 2. Scenario deteriorates: Response time doubles to 20ms
        Snapshot slowSnap = new Snapshot();
        for (int i = 0; i < 10; i++) slowSnap.add(20);

        // when: Updating limit under congestion
        // Calculation:
        // - Estimated QueueSize = 20 * (1 - 10/20) = 10
        // - Since 10 > Beta(6), the limit should decrease.
        int newLimit = algo.update(20, slowSnap);

        // then
        assertEquals(19, newLimit, "Limit should decrease when the queue size exceeds Beta (congestion).");
    }
}