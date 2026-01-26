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
    void should_HandleMassiveVirtualThreads_WithoutPinning() throws InterruptedException {
        // [Verification Goal]
        // Ensure that the Limiter handles 10,000 concurrent Virtual Threads efficiently
        // without causing "Platform Thread Pinning", which typically leads to deadlocks or timeouts.

        // given
        int virtualThreadCount = 10_000;
        AtomicLimiter limiter = new AtomicLimiter(new VegasLimitAlgorithm(), NanoClock.system());

        // Executor for Virtual Threads (Java 21 feature)
        ExecutorService vExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(virtualThreadCount);
        AtomicInteger processed = new AtomicInteger(0);

        long start = System.nanoTime();

        // when: Submitting 10,000 concurrent tasks
        for (int i = 0; i < virtualThreadCount; i++) {
            vExecutor.submit(() -> {
                try {
                    if (limiter.acquire()) {
                        try {
                            // Even with blocking operations (sleep), virtual threads should unmount properly.
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

        // then: Expect completion within 5 seconds.
        // If pinning occurs, this will likely timeout or take much longer.
        boolean finished = latch.await(5, TimeUnit.SECONDS);
        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        System.out.println("Processed: " + processed.get() + " / " + virtualThreadCount);
        System.out.println("Time taken: " + durationMs + "ms");

        assertTrue(finished, "10,000 Virtual Threads failed to complete within 5s (Potential Pinning detected).");
        assertEquals(virtualThreadCount, processed.get(), "All requests should have been attempted.");

        // Cleanup
        vExecutor.shutdown();
    }
}