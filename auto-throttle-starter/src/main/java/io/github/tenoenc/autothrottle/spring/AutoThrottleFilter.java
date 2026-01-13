package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A Servlet Filter that intercepts incoming HTTP requests to apply concurrency limits.
 * <p>
 * This filter acts as the entry point for the Auto Throttle mechanism. It delegates
 * the decision to proceed or reject the request to the {@link AtomicLimiter}.
 * </p>
 */
public class AutoThrottleFilter implements Filter {
    private final AtomicLimiter limiter;

    public AutoThrottleFilter(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 1. Attempt to acquire a permit.
        if (!limiter.acquire()) {
            // [Rejected] Concurrency limit exceeded.
            // Return HTTP 503 (Service Unavailable) to indicate temporary overload.
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(503, "The server is currently overloaded. Please try again later.");
            return; // Stop processing the request chain.
        }

        long start = System.nanoTime();
        try {
            // 2. Proceed with the request (Execute Controller).
            chain.doFilter(request, response);
        } finally {
            // 3. Release the permit and record execution time.
            // This is executed regardless of whether the request succeeded or failed (e.g., exception).
            // Future improvement: We might want to exclude specific HTTP errors (e.g., 500) from RTT calculations.
            limiter.release(start);
        }
    }
}