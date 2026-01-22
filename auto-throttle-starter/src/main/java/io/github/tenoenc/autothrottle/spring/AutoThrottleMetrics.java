package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Micrometer에 리미터의 내부 상태(Limit, Inflight 등)를 노출하는 바인더
 */
public class AutoThrottleMetrics implements MeterBinder {

    private final AtomicLimiter limiter;

    public AutoThrottleMetrics(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // 1. 현재 허용 한도
        Gauge.builder("auto.throttle.limit", limiter, AtomicLimiter::getLimit)
                .description("Current concurrency limit calculated by algorithm")
                .register(registry);

        // 2. 현재 처리 중인 요청 수
        Gauge.builder("auto.throttle.inflgiht", limiter, AtomicLimiter::getInflight)
                .description("Current number of requests being processed")
                .register(registry);
    }
}
