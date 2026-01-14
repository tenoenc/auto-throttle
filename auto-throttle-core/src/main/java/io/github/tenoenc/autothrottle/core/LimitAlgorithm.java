package io.github.tenoenc.autothrottle.core;

/**
 * 시스템 상태(Snapshot)를 보고, 다음 구간의 적정 동시성 한도(Limit)를 계산하는 함수
 */
@FunctionalInterface
public interface LimitAlgorithm {

    /**
     * @param currentLimit 현재 설정된 한도
     * @param snapshot     지난 구간 동안 수집된 통계 (RTT, 요청 수 등)
     * @return 갱신된 한도 값 (반드시 1 이상이어야 함)
     */
    int update(int currentLimit, Snapshot snapshot);
}
