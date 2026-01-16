package io.github.tenoenc.autothrottle.core;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLimiter {
    private final LimitAlgorithm algorithm;
    private final AtomicRingBuffer ringBuffer;
    private final Snapshot snapshot;
    private final NanoClock clock;

    // volatile: 여러 스레드가 동시에 읽을 때 항상 최신 값을 보장
    private volatile int limit;

    // 현재 처리 중인 요청 수 (동시성 제어의 핵심)
    private final AtomicInteger currentInflight = new AtomicInteger(0);

    // 마지막으로 리밋을 갱신한 시간 (Window 체크용)
    private final AtomicLong lastUpdateTime;

    // 리밋 갱신 중인지 표시하는 플래그 (스핀락 역할)
    private final AtomicInteger updateLock = new AtomicInteger(0);

    // 통계 수집 커서
    private long lastCursor = 0;

    // 설정값: 윈도우 크기 (100ms = 100,000,000ns)
    private static final long WINDOW_INTERVAL_NS = 100_000_000;

    public AtomicLimiter(LimitAlgorithm algorithm, NanoClock clock) {
        this.algorithm = algorithm;
        this.clock = clock;
        this.ringBuffer = new AtomicRingBuffer(4096);
        this.snapshot = new Snapshot();

        // 초기 리밋은 넉넉하게 시작 (Slow Start는 나중에 구현)
        this.limit = 20;
        this.lastUpdateTime = new AtomicLong(clock.nanoTime());
    }

    /**
     * 진입 요청 (Acquire)
     *
     * @return true=통과, false=차단 (Fast Failure)
     */
    public boolean acquire() {
        while (true) {
            int current = currentInflight.get();

            // 1. 한도 초과 체크
            if (current >= limit) {
                return false;
            }

            // 2. 카운터 증가 (CAS)
            // 동시에 여러 스레드가 들어올 때, 안전하게 하나만 증가시킴
            if (currentInflight.compareAndSet(current, current + 1)) {
                return true;
            }
            // CAS 실패 시 루프를 돌며 재시도 (매우 짧은 순간이라 성능 영향 미비)
        }
    }

    /**
     * 완료 처리 및 피드백 (Release)
     *
     * @param startNanos acquire 성공 시점의 시간
     */
    public void release(long startNanos) {
        // 1. 처리 중 카운터 감소
        currentInflight.decrementAndGet();

        long now = clock.nanoTime();
        long duration = now - startNanos;

        // 2. 수행 시간(RTT) 기록 (Zero-Allocation)
        ringBuffer.add(duration);

        // 3. 리밋 갱신 트리거 (Piggybacking)
        // 별도 스레드 없이, 요청을 마친 스레드가 시간이 됐는지 확인하고 갱신함
        tryUpdateLimit(now);
    }

    private void tryUpdateLimit(long now) {
        long lastUpdate = lastUpdateTime.get();

        // 윈도우 시간이 안 지났으면 패스 (빠른 리턴)
        if (now - lastUpdate < WINDOW_INTERVAL_NS) {
            return;
        }

        // 윈도우가 지났다면, 오직 한 스레드가 갱신 권한 획득 (CAS)
        if (updateLock.compareAndSet(0, 1)) {
            try {
                // 더블 체크 (그 사이 다른 스레드가 갱신했을 수도 있음)
                if (now - lastUpdateTime.get() < WINDOW_INTERVAL_NS) {
                    return;
                }

                // 4. 통계 수집 & 알고리즘 실행
                lastCursor = ringBuffer.collect(snapshot, lastCursor);
                int newLimit = algorithm.update(limit, snapshot);

                // 5. 리밋 반영 (volatile write)
                this.limit = newLimit;

                // 시간 갱신
                lastUpdateTime.set(now);
            } finally {
                // 락 해제
                updateLock.set(0);
            }
        }
    }

    // 테스트/모니터링용
    public int getLimit() { return limit; }
    public int getInflight() { return currentInflight.get(); }
}
