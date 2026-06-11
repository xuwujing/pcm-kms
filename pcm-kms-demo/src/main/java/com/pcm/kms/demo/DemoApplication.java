package com.pcm.kms.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PCM-KMS Demo 示例应用
 * <p>
 * 演示如何在一个普通 Spring Boot 业务项目中接入 PCM-KMS，使用 KmsClient 完成加解密、签名验签等操作。
 * <p>
 * 前置条件：先启动 pcm-kms-server（默认 http://localhost:8080），并在管理后台创建应用、启用应用、创建密钥。
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
