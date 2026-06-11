package com.pcm.kms.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * KMS 安全配置属性
 * <p>
 * 配置项：
 * - kms.security.strict-sign: 是否强制验签（默认 false，开发模式）
 * - kms.security.request-expire-seconds: 请求有效期秒数（默认 300）
 * - kms.ratelimit.enabled: 是否启用限流（默认 true）
 * - kms.ratelimit.max-per-minute: 每分钟最大请求数（默认 60）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kms")
public class KmsProperties {

    private Security security = new Security();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Security {
        /** 是否强制验签（开发模式建议关闭） */
        private boolean strictSign = false;
        /** 请求有效期（秒） */
        private int requestExpireSeconds = 300;
    }

    @Data
    public static class RateLimit {
        /** 是否启用限流 */
        private boolean enabled = true;
        /** 每分钟最大请求数 */
        private int maxPerMinute = 60;
    }
}
