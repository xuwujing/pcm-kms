package com.pcm.kms.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication(scanBasePackages = "com.pcm.kms")
@Slf4j
public class PcmKmsServerApplication {

    public static void main(String[] args) {
        // SQLite 模式下自动创建 data 目录，避免启动报路径不存在
        new File("./data").mkdirs();
        SpringApplication.run(PcmKmsServerApplication.class, args);
        log.info("启动成功");
    }
}
