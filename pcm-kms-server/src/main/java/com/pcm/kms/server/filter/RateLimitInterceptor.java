package com.pcm.kms.server.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private boolean enabled = true;
    private int maxPerMinute = 60;

    private final Map<String, WindowCounter> counterMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String key = resolveKey(request);
        WindowCounter counter = counterMap.computeIfAbsent(key, ignored -> new WindowCounter());
        if (counter.incrementAndGet() > maxPerMinute) {
            log.warn("rate limit triggered: key={}, count={}, max={}", key, counter.count.get(), maxPerMinute);
            writeLimitResponse(response);
            return false;
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId != null && !clientId.isEmpty()) {
            return "client:" + clientId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void writeLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"timestamp\":" + System.currentTimeMillis() + "}");
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    private static class WindowCounter {
        final AtomicLong count = new AtomicLong(0);
        volatile long windowStart = System.currentTimeMillis();

        long incrementAndGet() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60000) {
                synchronized (this) {
                    if (now - windowStart >= 60000) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet();
        }
    }
}
