package com.pcm.kms.stress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 压力测试启动类
 * <p>
 * 启动后访问 http://localhost:8082/stress/start 触发压力测试
 */
@SpringBootApplication
public class StressTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(StressTestApplication.class, args);
    }
}
