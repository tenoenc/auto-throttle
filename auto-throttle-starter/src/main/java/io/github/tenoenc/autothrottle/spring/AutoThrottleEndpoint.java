package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes Auto Throttle runtime information via Spring Boot Actuator.
 * <p>
 * Endpoint ID: {@code autothrottle}
 * <br>
 * Default Path: {@code /actuator/autothrottle}
 * </p>
 */
@Endpoint(id = "autothrottle")
public class AutoThrottleEndpoint {
    private final AtomicLimiter limiter;

    public AutoThrottleEndpoint(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    /**
     * Returns the current state of the limiter.
     *
     * @return A map containing 'limit' (current capacity) and 'inflight' (current load).
     */
    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("limit", limiter.getLimit());
        info.put("inflight", limiter.getInflight());
        // Additional algorithm details can be exposed here if needed.
        return info;
    }
}