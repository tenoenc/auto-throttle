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

    /**
     * 버퍼에 쌓인 데이터 중 읽지 않은 부분을 스냅샷에 집계합니다.
     *
     * @param targetSnapshot 결과를 담을 재사용 객체
     * @param lastCursor     마지막으로 읽었던 커서위치
     * @return 읽기를 마친 최신 커서 위치 (다음 호출 시 lastCursor로 사용)
     */
    public long collect(Snapshot targetSnapshot, long lastCursor) {
        long currentCursor = cursor.get();

        // 읽을 데이터가 없으면 바로 리턴
        if (currentCursor == lastCursor) {
            return currentCursor;
        }

        long countToRead = currentCursor - lastCursor;

        // 버퍼 크기 보다 더 많이 쌓였다면(덮어쓰기 발생),
        // 전체를 다 읽지 않고 최근 버퍼 크기만큼만 읽습니다. (오래된 데이터 버림)
        long readStart = lastCursor;
        if (countToRead > buffer.length) {
            readStart = currentCursor - buffer.length;
        }

        targetSnapshot.reset(); // 기존 데이터 초기화

        // 배열 순회 (Lock 없이 수행하므로 읽는 도중 값이 바뀔 수 있음 -> 허용)
        // High Performance Sampling에서는 약간의 오차보다 속도가 중요함
        for (long i = readStart; i < currentCursor; i++) {
            int index = (int) (i & mask);
            long value = buffer[index];
            targetSnapshot.add(value);
        }

        return currentCursor;
    }

    private boolean isPowerOfTwo(int number) {
        return number > 0 && (number & (number - 1)) == 0;
    }

    public long[] getBuffer() { return buffer; }
}
