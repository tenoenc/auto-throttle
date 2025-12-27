package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicLimiterTest {

    @Test
    void testAdaptiveLogics() {
        // 가짜 시간 소스 (시간을 조종하기 위함)
        TestClock clock = new TestClock();

        // 기본 알고리즘 (Vegas)
        VegasLimitAlgorithm algo = new VegasLimitAlgorithm(3, 6);
        AtomicLimiter limiter = new AtomicLimiter(algo, clock);

        // [Phase 1: 학습 구간] - 서버 빠름 (2ms)
        // 리밋(20)만큼 꽉 채워서 요청을 보냄 -> 큐 사이즈 0으로 인식 -> 리밋 증가 기대
        for (int i = 0; i < 60; i++) {
            if (limiter.acquire()) {
                clock.forward(2_000_000);
                limiter.release(clock.nanoTime() - 2_000_000);
            }
        }

        // 윈도우(100ms)를 넘기기 위해 시간 점프
        clock.forward(150_000_000);

        // 업데이트 트리거를 위해 한 번 더 요청
        if (limiter.acquire()) limiter.release(clock.nanoTime());

        // 확인: 리밋이 초기값(20)보다 커졌어야 함 (빠르니까)
        assertTrue(limiter.getLimit() > 20, "서버가 빠르면 리밋 증가: " + limiter.getLimit());
        int peakLimit = limiter.getLimit();

        // [Phase 2: 과부하 구간] - 서버 느려짐 (50ms로 지연 발생)
        // 100ms / 15ms = 6.6개 -> MIN_MINSAMPLES(5)를 넘겨서 알고리즘 동작함
        // Vegas 계산:
        // minRTT = 2ms
        // currentRTT = 15ms
        // QueueSize = Limit * (1 - 2/15) = Limit * 0.86
        // Limit이 20이라 치면 QueueSize 17.
        // 17 > Beta(6) 이므로 리밋 감소해야 함
        for (int i = 0; i < 100; i++) {
            if (limiter.acquire()) {
                clock.forward(15_000_000); // 15ms (평소보다 7.5배 느림)
                limiter.release(clock.nanoTime() - 15_000_000);
            }
        }

        // 윈도우 경과 및 업데이트 트리거
        clock.forward(150_000_000);
        if (limiter.acquire()) limiter.release(clock.nanoTime());

        // 확인: 리밋 감소 확인
        int finalLimit = limiter.getLimit();
        System.out.println("Peak Limit: " + peakLimit + " -> Final Limit: " + finalLimit);
        assertTrue(limiter.getLimit() < peakLimit, "서버가 느려지면 리밋 감소: " + limiter.getLimit());
    }

    // 테스트용 가짜 시계
    static class TestClock implements NanoClock {
        private long now = 0;

        @Override
        public long nanoTime() { return now; }
        public void forward(long nanos) { now += nanos; }
    }
}