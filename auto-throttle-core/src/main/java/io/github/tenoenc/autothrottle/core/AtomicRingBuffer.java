package io.github.tenoenc.autothrottle.core;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicRingBuffer implements MetricRegistry {
    private final long[] buffer;
    private final int mask;
    private final AtomicLong cursor = new AtomicLong(0);

    /**
     * @param bufferSize 버퍼 크기, 반드시 2의 거듭제곱이어야 함
     */
    public AtomicRingBuffer(int bufferSize) {
        if (!isPowerOfTwo(bufferSize)) {
            throw new IllegalArgumentException("버퍼 크기는 반드시 2의 거듭제곱이어야 합니다.");
        }
        this.buffer = new long[bufferSize];
        this.mask = bufferSize - 1; // 1024(100 0000 0000) -> 1023(011 1111 1111)
    }

    @Override
    public void add(long value) {
        // 1. 현재 커서 위치를 가져오고 1 증가 (Atomic)
        long currentCursor = cursor.getAndIncrement();

        // 2. 비트 연산으로 인덱스 계산 (Modulo 연산보다 빠름)
        // 예: 1025 & 1023 = 1 (배열의 1번 인덱스로 순환)
        int index = (int) (currentCursor & mask);

        // 3. 값 기록 (덮어쓰기)
        // Race Condition: 읽기/쓰기가 동시에 일어날 수 있지만,
        // 통계용 데이터(Sampling)이므로 약간의 오차는 성능을 위해 허용합니다.
        buffer[index] = value;
    }

    private boolean isPowerOfTwo(int number) {
        return number > 0 && (number & (number - 1)) == 0;
    }

    public long[] getBuffer() {
        return buffer;
    }
}
