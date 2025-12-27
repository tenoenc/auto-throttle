package io.github.tenoenc.autothrottle.core;

public class VegasLimitAlgorithm implements LimitAlgorithm {
    private final int alpha; // 큐사이즈 하향선 (이보다 적으면 Limit 증가)
    private final int beta; // 큐사이즈 상한선 (이보다 많으면 Limit 감소)

    // 서버가 경험한 '물리적 한계 속도(가증 빠른 응답)'를 기억합니다.
    private double minRtt = Double.MAX_VALUE;

    public VegasLimitAlgorithm() {
        this(3, 6); // 기본값: 대기열에 3~6개 정도 쌓이는 상태를 '최적'으로 간주
    }

    public VegasLimitAlgorithm(int alpha, int beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public int update(int currentLimit, Snapshot snapshot) {
        // 데이터가 신뢰할 수 없으면(노이즈) 변경 없이 리턴
        if (!snapshot.isReliable()) {
            return currentLimit;
        }

        double currentAvgRtt = snapshot.getAverage();
        if (currentAvgRtt <= 0) return currentLimit;

        // 1. 최소 Rtt 학습 (서버의 최고 성능 갱신)
        if (currentAvgRtt < minRtt) {
            minRtt = currentAvgRtt;
        }

        // 2. Vegas 공식: 지연 시간을 기반으로 '가상의 대기열 크기' 추정
        // 예: 리밋이 100인데, 평소보다 2배 느려졌다? -> 큐에 50개가 쌓인 셈이다.
        double queueSize = currentLimit * (1 - minRtt / currentAvgRtt);

        // 3. 임계값(Alpha/Beta)에 따른 제어
        int newLimit = currentLimit;

        if (queueSize < alpha) {
            // 너무 한가함 (대기열이 텅 빔) -> Limit 증가
            newLimit = currentLimit + 1;
        } else if (queueSize > beta) {
            // 너무 혼잡함 (대기열 폭발 직전) -> Limit 감소
            newLimit = currentLimit - 1;
        }
        // alpha ~ beta 사이면 현상 유지 (Stable)

        // 안전장치: 최소 1개의 요청은 받아야 함
        return Math.max(1, newLimit);
    }
}
