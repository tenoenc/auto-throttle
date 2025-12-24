package io.github.tenoenc.autothrottle.benchmark;

import io.github.tenoenc.autothrottle.core.AtomicRingBuffer;
import io.github.tenoenc.autothrottle.core.Snapshot;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime) // 이번엔 수행 시간(Latency) 위주로 측정
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RingBufferBenchmark {
    private AtomicRingBuffer ringBuffer;
    private Snapshot snapshot;
    private long lastCursor;

    @Setup
    public void setup() {
        // 4096 크기의 버퍼 생성
        ringBuffer = new AtomicRingBuffer(4096);
        snapshot = new Snapshot();

        // 미리 데이터를 좀 채워둡니다.
        for (int i = 0; i < 4096; i++) {
            ringBuffer.add(100 + i);
        }
        lastCursor = 0;
    }

    @Benchmark
    public void writeBenchmark(Blackhole blackhole) {
        // 기존 쓰기 테스트 (비교용)
        ringBuffer.add(100);
    }

    @Benchmark
    public void collectBenchmark(Blackhole blackhole) {
        // 읽기 테스트
        // 실제로는 커서가 이동해야 읽을 게 생기지만,
        // 벤치마크에서는 강제로 전체 버퍼(4096개)를 다 읽는 최악의 경우를 시뮬레이션 하기 위해
        // 내부 로직을 복사하거나, 매번 add를 호출해줘야 함.

        // 여기서는 "쓰기 1회 + 읽기 시도"를 한 세트로 묶어서 측정합니다.
        ringBuffer.add(123);

        // collect 호출 (Snapshot 객체 재사용 확인)
        long currentCursor = ringBuffer.collect(snapshot, lastCursor);

        // 커서 업데이트 (다음 읽기를 위해)
        lastCursor = currentCursor;

        // JIT 컴파일러가 결과를 사용하지 않는 걸 확인하고 코드를 삭제하는 걸 방지
        blackhole.consume(snapshot);
    }
}
