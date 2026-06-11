package com.pcm.kms.server.filter;

import com.pcm.kms.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流拦截器
 * <p>
 * 基于客户端 IP 或 clientId 进行分钟级限流。使用内存滑动窗口实现，
 * 后续版本可切换为 Redis 实现以支持分布式。
 * <p>
 * 配置项：
 * - kms.ratelimit.enabled: 是否启用限流（默认 true）
 * - kms.ratelimit.max-per-minute: 每分钟最大请求数（默认 60）
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 每分钟最大请求数 */
    private int maxPerMinute = 60;

    /** 滑动窗口计数器：key = clientId 或 IP，value = 当前分钟内的请求计数 */
    private final Map<String, WindowCounter> counterMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = resolveKey(request);
        WindowCounter counter = counterMap.computeIfAbsent(key, k -> new WindowCounter());

        if (counter.incrementAndGet() > maxPerMinute) {
            log.warn("限流触发: key={}, count={}, max={}", key, counter.count.get(), maxPerMinute);
            writeLimitResponse(response);
            return false;
        }

        return true;
    }

    /**
     * 解析限流 Key：优先使用 X-Client-Id 头，否则使用 IP
     */
    private String resolveKey(HttpServletRequest request) {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId != null && !clientId.isEmpty()) {
            return "client:" + clientId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * 写入限流响应
     */
    private void writeLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<?> body = ApiResponse.error(429, "请求过于频繁，请稍后再试");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"timestamp\":" + System.currentTimeMillis() + "}");
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * 滑动窗口计数器：每分钟自动重置
     */
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
