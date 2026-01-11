package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicRingBufferTest {
    @Test
    void testCollectMetrics() {
        // given
        int size = 1024;
        AtomicRingBuffer ringBuffer = new AtomicRingBuffer(size);
        Snapshot snapshot = new Snapshot();

        // when: 데이터 3개 기록 (100, 200, 300)
        ringBuffer.add(100);
        ringBuffer.add(200);
        ringBuffer.add(300);

        // then: 0번 커서부터 읽기
        long nextCursor = ringBuffer.collect(snapshot, 0);

        assertEquals(3, nextCursor, "커서는 3만큼 이동해야 함");
        assertEquals(3, snapshot.totalCount, "데이터 개수는 3개");
        assertEquals(200.0, snapshot.getAverage(), "평균은 200이어야 함");
        assertEquals(300, snapshot.max, "최대는 300");
    }

    @Test
    void testBufferOverwrite() {
        // given: 크기가 4인 아주 작은 버퍼
        AtomicRingBuffer ringBuffer = new AtomicRingBuffer(4);
        Snapshot snapshot = new Snapshot();

        // when: 5개 넣음 (0, 1, 2, 3, 4) -> 0은 덮어쓰여짐
        for (int i = 0; i < 5; i++) {
            ringBuffer.add(i);
        }

        // then: 처음부터 읽으려고 시도
        long nextCursor = ringBuffer.collect(snapshot, 0);

        // 버퍼 크기(4)만큼만 읽혀야 함 (최신 데이터 1, 2, 3, 4)
        assertEquals(5, nextCursor); // 커서는 5까지 갔음
        assertEquals(4, snapshot.totalCount, "덮어쓰기 발생 시 버퍼 크기만큼만 읽어야 함");
        assertEquals(4, snapshot.max); // 마지막 값
        assertEquals(1, snapshot.min); // 0은 사라지고 1부터 시작
    }
}