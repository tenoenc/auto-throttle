package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import io.github.tenoenc.autothrottle.core.LimitAlgorithm;
import io.github.tenoenc.autothrottle.core.NanoClock;
import io.github.tenoenc.autothrottle.core.VegasLimitAlgorithm;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import jakarta.websocket.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@ConditionalOnClass(AtomicLimiter.class)
@EnableConfigurationProperties(AutoThrottleProperties.class)
@ConditionalOnProperty(prefix = "auto-throttle", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AutoThrottleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LimitAlgorithm.class)
    public LimitAlgorithm limitAlgorithm(AutoThrottleProperties props) {
        return new VegasLimitAlgorithm(props.getAlpha(), props.getBeta());
    }

    @Bean
    @ConditionalOnMissingBean(NanoClock.class)
    public NanoClock nanoClock() {
        return NanoClock.system();
    }

    @Bean
    @ConditionalOnMissingBean(AtomicLimiter.class)
    public AtomicLimiter atomicLimiter(LimitAlgorithm algorithm, NanoClock clock) {
        return new AtomicLimiter(algorithm, clock);
    }

    @Bean
    public FilterRegistrationBean<AutoThrottleFilter> autoThrottleFilter(AtomicLimiter limiter) {
        FilterRegistrationBean<AutoThrottleFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new AutoThrottleFilter(limiter));

        // 모든 URL 패턴에 대해 적용 (나중에 프로퍼티로 조절 가능)
        registration.addUrlPatterns("/*");

        // 필터 순서 설정
        // 가장 먼저 실행되어야 불필요한 연산을 막을 수 있음 (Ordered.HIGHEST_PRECEDENCE)
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    public AutoThrottleMetrics autoThrottleMetrics(AtomicLimiter limiter) {
        return new AutoThrottleMetrics(limiter);
    }

    @Bean
    @ConditionalOnClass(Endpoint.class)
    public AutoThrottleEndpoint autoThrottleEndpoint(AtomicLimiter limiter) {
        return new AutoThrottleEndpoint(limiter);
    }
}
