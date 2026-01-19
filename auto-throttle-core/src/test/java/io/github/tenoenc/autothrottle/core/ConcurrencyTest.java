package io.github.tenoenc.autothrottle.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyTest {
    @Test
    void testConcurrencySafety() throws InterruptedException {
        // given
        int threadCount = 32; // 동시에 공격할 스레드 수
        int requestPerThread = 10_000; // 스레드당 요청 횟수

        // 실제 시간 사용 (NanoClock.system())
        AtomicLimiter limiter = new AtomicLimiter(new VegasLimitAlgorithm(), NanoClock.system());

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작할 때까지 대기

                    for (int j = 0; j < requestPerThread; j++) {
                        if (limiter.acquire()) {
                            successCount.incrementAndGet();
                            try {
                                // 스레드 경합 시뮬레이션 (100,000ns)
                                // Thread.sleep은 너무 느리므로 바쁜 대기 (Busy wait) 사용
                                long end = System.nanoTime() + 100_000;
                                while (System.nanoTime() < end) ;
                            } finally {
                                // 성공했으면 반드시 release 해야 함
                                limiter.release(System.nanoTime() - 100_000);
                            }
                        } else {
                            rejectCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 동시 시작
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        System.out.println("Total Request: " + (threadCount * requestPerThread));
        System.out.println("Success: " + successCount.get());
        System.out.println("Rejected: " + rejectCount.get());
        System.out.println("Final Inflight: " + limiter.getInflight());
        System.out.println("Final Limit: " + limiter.getLimit());

        // 1. 카운트 누수 검증
        assertEquals(0, limiter.getInflight(), "모든 요청이 끝나면 Inflight는 0이어야 함");

        // 2. 총 요청 수 검증
        assertEquals(threadCount * requestPerThread, successCount.get() + rejectCount.get());

        // 3. 리미터가 동작했는지 검증 (무조건 다 성공하거나 다 실패하면 안 됨)
        // 트래픽이 많았으므로 적당히 차단되었어야 함
        assertTrue(rejectCount.get() > 0, "부하가 심하면 일부 요청은 거절되어야 함");
        assertTrue(successCount.get() > 0, "일부 요청은 처리되어야 함");
    }
}
