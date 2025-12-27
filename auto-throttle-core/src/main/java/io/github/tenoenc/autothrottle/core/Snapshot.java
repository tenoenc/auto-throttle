package io.github.tenoenc.autothrottle.core;

/**
 * 특정 구간의 통계 데이터를 담는 컨테이너
 * GC 방지를 위해 매번 생성하지 않고, reset() 후 값을 채워 재사용합니다.
 */
public class Snapshot {
    // 노이즈 필터링 상수 정의
    // 최소한 이 개수 이상의 표본이 있어야 통계로 인정함
    private static final int MIN_SAMPLES = 5;

    // public 필드로 직접 접근하여 Getter/Setter 오버헤드 제거
    public long totalCount;
    public long totalSum;
    public long min;
    public long max;

    public Snapshot() {
        reset();
    }

    public void reset() {
        this.totalCount = 0;
        this.totalSum = 0;
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
    }

    public void add(long value) {
        this.totalCount++;
        this.totalSum += value;
        if (value < this.min) this.min = value;
        if (value > this.max) this.max = value;
    }

    // 평균 계산 (나눗셈 0 방지)
    public double getAverage() {
        return totalCount == 0 ? 0 : (double) totalSum / totalCount;
    }

    /**
     * 현재 스냅샷이 제어 알고리즘에 사용할 만큼 충분한 데이터를 가졌는지 판단.
     * 데이터가 너무 적으면(노이즈) 알고리즘을 돌리지 않고 이전 상태를 유지해야 함.
     */
    public boolean isReliable() {
        return totalCount >= MIN_SAMPLES;
    }

    @Override
    public String toString() {
        return "Snapshot{cnt=" + totalCount + ", avg=" + getAverage() + ", valid=" + isReliable() + "}";
    }
}
