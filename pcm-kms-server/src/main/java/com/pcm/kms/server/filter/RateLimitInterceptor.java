package com.pcm.kms.server.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流拦截器
 * <p>
 * 基于 IP 或 clientId 的分钟级滑动窗口限流。
 * 使用 Caffeine 缓存管理计数器，1 分钟不活跃后自动清理，避免内存泄漏。
 * <p>
 * 限流维度：
 * <ul>
 *   <li>优先使用 X-Client-Id Header 作为限流 key</li>
 *   <li>无 ClientId 时降级为 IP 限流</li>
 * </ul>
 * <p>
 * 配置项：
 * <ul>
 *   <li>kms.ratelimit.enabled: 是否启用限流（默认 true）</li>
 *   <li>kms.ratelimit.max-per-minute: 每分钟最大请求数（默认 60）</li>
 * </ul>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private boolean enabled = true;
    private int maxPerMinute = 60;

    /**
     * 限流计数器缓存
     * <p>
     * key 为 "client:{clientId}" 或 "ip:{remoteAddr}"，value 为窗口计数器。
     * 1 分钟不活跃后自动过期，避免内存泄漏。
     * 最大缓存 10000 个 key，超出后按 LRU 淘汰。
     */
    private final Cache<String, WindowCounter> counterCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String key = resolveKey(request);
        WindowCounter counter = counterCache.get(key, k -> new WindowCounter());
        if (counter != null && counter.incrementAndGet() > maxPerMinute) {
            log.warn("rate limit triggered: key={}, count={}, max={}", key, counter.count.get(), maxPerMinute);
            writeLimitResponse(response);
            return false;
        }
        return true;
    }

    /**
     * 解析限流 key
     *
     * @param request HTTP 请求
     * @return 限流标识（优先 clientId，降级 IP）
     */
    private String resolveKey(HttpServletRequest request) {
        String clientId = request.getHeader("X-Client-Id");
        if (clientId != null && !clientId.isEmpty()) {
            return "client:" + clientId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * 写入限流响应（HTTP 429）
     */
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

    /**
     * 滑动窗口计数器
     * <p>
     * 内部使用 double-check locking 保证线程安全的窗口重置。
     */
    private static class WindowCounter {
        final AtomicLong count = new AtomicLong(0);
        volatile long windowStart = System.currentTimeMillis();

        /**
         * 自增并返回当前计数值
         * <p>
         * 如果距离窗口开始超过 60 秒，重置计数器和窗口起始时间。
         */
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
