package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VegasLimitAlgorithmTest {

    @Test
    void testVegasIncrease() {
        // given: 현재 리밋 10, 최적 상태(3~6)
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);
        Snapshot snapshot = new Snapshot();

        // 데이터 채우기 (노이즈 필터 통과 위해 5개 이상)
        // 상황: 아주 빠름 (평균 10ms)
        for (int i = 0; i < 10; i++) snapshot.add(10);

        // when
        int newLimit = algo.update(10, snapshot);

        // then: minRTT가 10ms로 학습되고, 현재도 10ms이므로
        // queueSize = 10 * (1 - 10/10) = 0
        // 0 < alpha(3) 이므로 리밋 증가해야 함
        assertEquals(11, newLimit, "서버가 빠르면 리밋을 늘려야 함");
    }

    @Test
    void testVegasDecrease() {
        // given
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);

        // 1. 먼저 학습 시킴 (minRTT = 10ms)
        Snapshot fastSnap = new Snapshot();
        for (int i = 0; i < 10; i++) fastSnap.add(10);
        algo.update(10, fastSnap);

        // 2. 상황 악화: 응답 속도가 20ms 느려짐 (2배 지연)
        Snapshot slowSnap = new Snapshot();
        for (int i = 0; i < 10; i++) slowSnap.add(20);

        // when
        // 예상 QueueSize = 20 * (1 - 10/20) = 10
        // 10 > beta(6) 이므로 리밋 감소
        int newLimit = algo.update(20, slowSnap);

        // then
        assertEquals(19, newLimit, "서버가 느려지면 리밋을 줄여야 함");
    }
}
