package com.pcm.kms.common.util;

import java.util.UUID;

/**
 * ID 生成工具
 */
public class IdUtil {

    /**
     * 生成 clientId（kms_ 前缀 + 16位随机字符）
     */
    public static String generateClientId() {
        return "kms_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成 clientSecret（32位随机字符）
     */
    public static String generateClientSecret() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 生成 secretId（sid_ 前缀 + 16位随机字符）
     */
    public static String generateSecretId() {
        return "sid_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
