package com.pcm.kms.server.config;

import com.pcm.kms.server.filter.RateLimitInterceptor;
import com.pcm.kms.server.filter.SignatureVerifyInterceptor;
import com.pcm.kms.server.filter.AdminAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final KmsProperties kmsProperties;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final SignatureVerifyInterceptor signatureVerifyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        rateLimitInterceptor.setEnabled(kmsProperties.getRateLimit().isEnabled());
        rateLimitInterceptor.setMaxPerMinute(kmsProperties.getRateLimit().getMaxPerMinute());
        signatureVerifyInterceptor.setStrictSign(kmsProperties.getSecurity().isStrictSign());
        signatureVerifyInterceptor.setRequestExpireSeconds(kmsProperties.getSecurity().getRequestExpireSeconds());

        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);

        registry.addInterceptor(signatureVerifyInterceptor)
                .addPathPatterns("/api/crypto/**")
                .order(3);
    }
}
