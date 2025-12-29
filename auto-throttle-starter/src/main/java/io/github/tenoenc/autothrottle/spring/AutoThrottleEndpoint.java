package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.HashMap;
import java.util.Map;

@Endpoint(id = "autothrottle")
public class AutoThrottleEndpoint {
    private final AtomicLimiter limiter;

    public AutoThrottleEndpoint(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    @ReadOperation
    public Map<String, Object> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("limit", limiter.getLimit());
        info.put("inflight", limiter.getInflight());
        // 필요하다면 알고리즘
        return info;
    }
}
