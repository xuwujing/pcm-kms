package com.pcm.kms.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.pcm.kms")
public class PcmKmsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PcmKmsServerApplication.class, args);
    }
}
