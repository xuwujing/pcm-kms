package com.pcm.kms.server.config;

import com.pcm.kms.core.service.RuntimeInfoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreBeansConfig {

    @Bean
    public RuntimeInfoService runtimeInfoService() {
        return new RuntimeInfoService();
    }
}
