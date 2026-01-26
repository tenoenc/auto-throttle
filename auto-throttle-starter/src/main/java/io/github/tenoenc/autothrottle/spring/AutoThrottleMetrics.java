package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * A Micrometer binder that exposes the internal state of the limiter.
 * <p>
 * Registered Metrics:
 * <ul>
 * <li>{@code auto.throttle.limit}: The current dynamic concurrency limit.</li>
 * <li>{@code auto.throttle.inflight}: The number of requests currently being processed.</li>
 * </ul>
 * </p>
 */
public class AutoThrottleMetrics implements MeterBinder {

    private final AtomicLimiter limiter;

    public AutoThrottleMetrics(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // 1. Expose current limit.
        Gauge.builder("auto.throttle.limit", limiter, AtomicLimiter::getLimit)
                .description("Current concurrency limit calculated by algorithm")
                .register(registry);

        // 2. Expose current in-flight requests.
        // Typo fix: inflgiht -> inflight
        Gauge.builder("auto.throttle.inflight", limiter, AtomicLimiter::getInflight)
                .description("Current number of requests being processed")
                .register(registry);
    }
}