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

/**
 * Spring Boot Auto-Configuration for Auto Throttle.
 * <p>
 * This configuration is automatically loaded when {@code AtomicLimiter} is on the classpath.
 * It can be disabled by setting {@code auto-throttle.enabled=false}.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(AtomicLimiter.class)
@EnableConfigurationProperties(AutoThrottleProperties.class)
@ConditionalOnProperty(prefix = "auto-throttle", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AutoThrottleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LimitAlgorithm.class)
    public LimitAlgorithm limitAlgorithm(AutoThrottleProperties props) {
        // Default to TCP Vegas algorithm with configured alpha/beta values.
        return new VegasLimitAlgorithm(props.getAlpha(), props.getBeta());
    }

    @Bean
    @ConditionalOnMissingBean(NanoClock.class)
    public NanoClock nanoClock() {
        // Use system nano time by default.
        return NanoClock.system();
    }

    @Bean
    @ConditionalOnMissingBean(AtomicLimiter.class)
    public AtomicLimiter atomicLimiter(LimitAlgorithm algorithm, NanoClock clock) {
        return new AtomicLimiter(algorithm, clock);
    }

    /**
     * Registers the {@link AutoThrottleFilter} with the highest precedence.
     * <p>
     * <strong>Why Highest Precedence?</strong>
     * The throttle filter must execute before any other resource-intensive logic
     * (e.g., security checks, parsing, database access) to effectively protect the server
     * and reject excess traffic as early as possible (Fail-Fast).
     * </p>
     */
    @Bean
    public FilterRegistrationBean<AutoThrottleFilter> autoThrottleFilter(AtomicLimiter limiter) {
        FilterRegistrationBean<AutoThrottleFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(new AutoThrottleFilter(limiter));

        // Apply to all URL patterns by default.
        // TODO: This could be made configurable via properties in the future.
        registration.addUrlPatterns("/*");

        // Set order to HIGHEST_PRECEDENCE to ensure it runs first.
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