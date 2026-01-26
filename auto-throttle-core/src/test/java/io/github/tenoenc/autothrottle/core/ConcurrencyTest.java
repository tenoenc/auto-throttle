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
    void should_MaintainSafetyAndNoLeaks_UnderHighConcurrency() throws InterruptedException {
        // given
        int threadCount = 32; // Number of concurrent threads
        int requestPerThread = 10_000; // Requests per thread

        // Use system clock for real-time simulation
        AtomicLimiter limiter = new AtomicLimiter(new VegasLimitAlgorithm(), NanoClock.system());

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectCount = new AtomicInteger(0);

        // when: Submitting concurrent load
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for the start signal

                    for (int j = 0; j < requestPerThread; j++) {
                        if (limiter.acquire()) {
                            successCount.incrementAndGet();
                            try {
                                // Simulation of thread contention (CPU-bound work 100,000ns)
                                // Busy-waiting is used instead of Thread.sleep for precision.
                                long end = System.nanoTime() + 100_000;
                                while (System.nanoTime() < end) ;
                            } finally {
                                // Release MUST be called upon success
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

        // Trigger simultaneous start
        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: Print results for manual inspection
        System.out.println("Total Request: " + (threadCount * requestPerThread));
        System.out.println("Success: " + successCount.get());
        System.out.println("Rejected: " + rejectCount.get());
        System.out.println("Final Inflight: " + limiter.getInflight());
        System.out.println("Final Limit: " + limiter.getLimit());

        // 1. Verify zero leakage
        assertEquals(0, limiter.getInflight(), "Inflight counter should return to 0 after all requests are done.");

        // 2. Verify total count
        assertEquals(threadCount * requestPerThread, successCount.get() + rejectCount.get(), "Total requests processed should match.");

        // 3. Verify limiter engagement
        // Under heavy load, the limiter should reject some requests to protect the system.
        assertTrue(rejectCount.get() > 0, "Some requests should be rejected under heavy load.");
        assertTrue(successCount.get() > 0, "Some requests should be successfully processed.");
    }
}