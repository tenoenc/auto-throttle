package io.github.tenoenc.autothrottle.core;

/**
 * 성능 지표(RTT 등)를 기록하는 저장소 인터페이스
 */
public interface MetricRegistry {

    /**
     * 측정된 값(예: RTT, nanoseconds)를 기록합니다.
     * 구현체는 이 메서드 호출 시 힙 메모리 할당(new)을 절대 하지 않아야 합니다.
     *
     * @param value 측정 값
     */
    void add(long value);
}
