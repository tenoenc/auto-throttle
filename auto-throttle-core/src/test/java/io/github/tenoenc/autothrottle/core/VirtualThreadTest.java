package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualThreadTest {

    @Test
    void testMassiveVirtualThreads() throws InterruptedException {
        // [검증 목표]
        // OS 스레드(Platform Thread)로는 감당하기 힘든 10,000개의 스레드를 생성해도
        // Limiter가 병목 없이(Pinning 없이) 잘 처리하는가?

        // given
        int virtualThreadCount = 10_000; // 1만 개의 가상 스레드
        AtomicLimiter limiter = new AtomicLimiter(new VegasLimitAlgorithm(), NanoClock.system());

        // Java 21의 가상 스레드 실행기 생성
        ExecutorService vExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(virtualThreadCount);
        AtomicInteger processed = new AtomicInteger(0);

        long start = System.nanoTime();

        // when: 1만 개 동시 투하
        for (int i = 0; i < virtualThreadCount; i++) {
            vExecutor.submit(() -> {
                try {
                    if (limiter.acquire()) {
                        try {
                            // 짧은 작업 수행 (Blocking이 있어도 가상 스레드는 잘 처리해야 함)
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            // ignore
                        } finally {
                            limiter.release(System.nanoTime() - 1_000_000);
                        }
                    }
                    processed.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // then: 5초 안에 다 끝나야 함 (Pinning 걸리면 훨씬 오래 걸림)
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("Processed: " + processed.get() + " / " + virtualThreadCount);
        System.out.println("Time taken: " + durationMs + "ms");

        assertTrue(finished, "가상 스레드 10,000개가 제시간(5초) 안에 처리를 못 끝냈음 (Pinning 의심)");
        assertEquals(virtualThreadCount, processed.get(), "모든 요청이 실행 시도는 되었어야 함");

        // 리소스 정리
        vExecutor.shutdown();
    }
}
