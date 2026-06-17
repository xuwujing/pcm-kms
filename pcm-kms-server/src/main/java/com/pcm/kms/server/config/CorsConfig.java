package com.pcm.kms.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域配置
 * <p>
 * 通过配置项控制允许的域名，避免生产环境使用通配符。
 * <p>
 * 配置项：
 * <ul>
 *   <li>kms.cors.allowed-origins: 允许的域名列表（逗号分隔，默认 * 开发模式）</li>
 * </ul>
 * <p>
 * 生产环境示例：
 * <pre>
 * kms:
 *   cors:
 *     allowed-origins: https://admin.example.com,https://portal.example.com
 * </pre>
 */
@Configuration
public class CorsConfig {

    @Value("${kms.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 解析允许的域名列表
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        if (origins.contains("*")) {
            config.addAllowedOriginPattern("*");
        } else {
            origins.forEach(o -> config.addAllowedOriginPattern(o.trim()));
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
