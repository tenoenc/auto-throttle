package io.github.tenoenc.autothrottle.spring;

import io.github.tenoenc.autothrottle.core.AtomicLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AutoThrottleFilter implements Filter {
    private final AtomicLimiter limiter;

    public AutoThrottleFilter(AtomicLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 1. 입장 시도
        if (!limiter.acquire()) {
            // [차단] 동시성 한도 초과
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(503, "서버가 혼잡하여 자동으로 요청이 제한되었습니다.");
            return; // 컨트롤러 실행 안 함
        }

        long start = System.nanoTime();
        try {
            // 2. 통과 (컨트롤러 실행)
            chain.doFilter(request, response);
        } finally {
            // 3. 퇴장 (성공이든 에러든 무조건 시간 기록)
            // 나중에 여기서 HTTP 500 에러 등을 구분해서 RTT 집계에서 뺄 수도 있음
            limiter.release(start);
        }
    }
}
