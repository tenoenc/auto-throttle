package io.github.tenoenc.autothrottle.benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EnvironmentBenchmark {

    @Benchmark
    public long baseline() {
        // 아무것도 안 하는 메서드의 호출 비용 측정 (영점 조정용)
        return System.nanoTime();
    }
}
