package com.pcm.kms.server.config;

import com.pcm.kms.server.filter.RateLimitInterceptor;
import com.pcm.kms.server.filter.SignatureVerifyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 注册拦截器：
 * - 限流拦截器：拦截所有 /api/** 路径
 * - 签名验证拦截器：拦截 /api/crypto/** 路径（仅 strict-sign=true 时生效）
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final SignatureVerifyInterceptor signatureVerifyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流：所有 API 路径
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 签名验证：仅加密服务路径
        registry.addInterceptor(signatureVerifyInterceptor)
                .addPathPatterns("/api/crypto/**")
                .order(2);
    }
}
