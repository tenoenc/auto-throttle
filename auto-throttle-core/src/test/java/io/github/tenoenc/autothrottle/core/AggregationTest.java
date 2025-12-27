package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AggregationTest {

    @Test
    void testNoiseFiltering() {
        // given
        Snapshot snapshot = new Snapshot();

        // when: 데이터가 3개뿐일 때 (MIN_SAMPLES = 5 미만)
        snapshot.add(100);
        snapshot.add(100);
        snapshot.add(100);

        // then: 신뢰할 수 없는 데이터로 판단해야 함
        assertFalse(snapshot.isReliable(), "샘플이 적으면 신뢰할 수 없음");

        // when: 데이터를 더 추가해서 5개가 넘어가면
        snapshot.add(100);
        snapshot.add(100);
        snapshot.add(100); // 총 6개

        // then: 신뢰할 수 있음
        assertTrue(snapshot.isReliable(), "샘플이 충분하면 신뢰할 수 있음");
    }

    @Test
    void testWindowAggregationSimulation() {
        // 시간 경계(Boundary) 시뮬레이션
        // 실제로는 Limiter 클래스 내부에서 시간을 체크하지만,
        // 여기서는 논리적으로 '구간'이 나뉘는지 테스트합니다.

        AtomicRingBuffer buffer = new AtomicRingBuffer(1024);
        Snapshot snapshot = new Snapshot();
        long lastCursor = 0;

        // --- 구간 1 (0ms ~ 100ms) ---
        // 트래픽 발생
        for (int i = 0; i < 10; i++) buffer.add(10); // 10ns 10번

        // 집계 수행
        lastCursor = buffer.collect(snapshot, lastCursor);

        assertTrue(snapshot.isReliable());
        assertEquals(10.0, snapshot.getAverage());

        // --- 구간 2 (100ms ~ 200ms) ---
        // 트래픽이 뜸함 (노이즈)
        buffer.add(50); // 1건 추가

        // 집계 수행
        lastCursor = buffer.collect(snapshot, lastCursor);

        assertEquals(1, snapshot.totalCount);
        assertFalse(snapshot.isReliable(), "구간 2는 샘플이 적어서 무시해야 함");
    }
}
