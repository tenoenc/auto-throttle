package io.github.tenoenc.autothrottle.benchmark;

import io.github.tenoenc.autothrottle.core.AtomicRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput) // 초당 처리량 측정
@OutputTimeUnit(TimeUnit.SECONDS)
public class RingBufferBenchmark {
    private AtomicRingBuffer ringBuffer;
    
    @Setup
    public void setup() {
        // 4096 크기의 버퍼 생성
        ringBuffer = new AtomicRingBuffer(4096);
    }
    
    @Benchmark
    public void writeBenchmark(Blackhole blackhole) {
        // 가상의 RTT 값(100)을 계속 기록
        ringBuffer.add(100);
    }
}
