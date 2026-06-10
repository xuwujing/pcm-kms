package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;

/**
 * SM3 国密摘要策略
 */
@Component
public class Sm3AlgorithmStrategy implements AlgorithmStrategy {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SM3;
    }

    @Override
    public String encrypt(String plainText, String key) {
        throw new UnsupportedOperationException("SM3 不支持加密");
    }

    @Override
    public String decrypt(String cipherText, String key) {
        throw new UnsupportedOperationException("SM3 不支持解密");
    }

    @Override
    public String digest(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SM3", BouncyCastleProvider.PROVIDER_NAME);
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SM3摘要失败", e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        throw new UnsupportedOperationException("SM3 不需要密钥");
    }
}
