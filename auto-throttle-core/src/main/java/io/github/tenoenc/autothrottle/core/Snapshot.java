package io.github.tenoenc.autothrottle.core;

/**
 * 특정 구간의 통계 데이터를 담는 컨테이너
 * GC 방지를 위해 매번 생성하지 않고, reset() 후 값을 채워 재사용합니다.
 */
public class Snapshot {
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

    @Override
    public String toString() {
        return "Snapshot{cnt=" + totalCount + ", avg=" + getAverage() + ", max=" + max + "}";
    }
}
