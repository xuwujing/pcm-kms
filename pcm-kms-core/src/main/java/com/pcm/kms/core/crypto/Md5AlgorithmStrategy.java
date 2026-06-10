package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * MD5 摘要策略
 */
@Component
public class Md5AlgorithmStrategy implements AlgorithmStrategy {

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.MD5;
    }

    @Override
    public String encrypt(String plainText, String key) {
        throw new UnsupportedOperationException("MD5 不支持加密");
    }

    @Override
    public String decrypt(String cipherText, String key) {
        throw new UnsupportedOperationException("MD5 不支持解密");
    }

    @Override
    public String digest(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("MD5摘要失败", e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        throw new UnsupportedOperationException("MD5 不需要密钥");
    }
}
