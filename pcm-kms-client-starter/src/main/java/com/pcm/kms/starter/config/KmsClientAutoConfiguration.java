package com.pcm.kms.starter.config;

import com.pcm.kms.starter.KmsClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * KMS 客户端自动配置
 */
@Configuration
@EnableConfigurationProperties(KmsClient.class)
public class KmsClientAutoConfiguration {

    @Bean
    public KmsClient kmsClient() {
        return new KmsClient();
    }
}
