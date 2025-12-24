package io.github.tenoenc.autothrottle.core;

/**
 * 시간 측정 소스 추상화
 * System.nanoTime() 호출 비용조자 아끼거나
 * 테스트 시 시간을 멈추기 위해 인터페이스로 분리합니다.
 */
@FunctionalInterface
public interface NanoClock {

    long nanoTime();

    // 기본 구현체: JVM 표준
    static NanoClock system() {
        return System::nanoTime;
    }
}