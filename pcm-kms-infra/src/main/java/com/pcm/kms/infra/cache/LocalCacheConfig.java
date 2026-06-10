package com.pcm.kms.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Redis 不可用时降级使用 Caffeine）
 */
@Configuration
public class LocalCacheConfig {

    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }
}
