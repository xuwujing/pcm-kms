package com.pcm.kms.server.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 请求体缓存过滤器
 * <p>
 * 将 {@link HttpServletRequest} 包装为 {@link ContentCachingRequestWrapper}，
 * 使请求体可以被多次读取（原始 InputStream 只能读一次）。
 * <p>
 * 主要用途：
 * <ul>
 *   <li>签名验证拦截器需要读取 body 来计算签名</li>
 *   <li>Controller 也需要读取 body 来反序列化参数</li>
 * </ul>
 * <p>
 * 设置为最高优先级（Order=1），确保在所有拦截器之前执行。
 */
@Component
@Order(1)
public class ContentCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 只对 POST 请求的 /api/crypto/ 路径进行 body 缓存
        // 避免对文件上传等场景产生不必要的内存开销
        if ("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/crypto/")) {
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
