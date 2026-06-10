package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * SM4 国密对称加密策略
 */
@Component
public class Sm4AlgorithmStrategy implements AlgorithmStrategy {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/ECB/PKCS5Padding";

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SM4;
    }

    @Override
    public String encrypt(String plainText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    @Override
    public String decrypt(String cipherText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4解密失败", e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            keyGen.init(128);
            byte[] keyBytes = keyGen.generateKey().getEncoded();
            KeyMaterial km = new KeyMaterial();
            km.setSecretKey(Base64.getEncoder().encodeToString(keyBytes));
            return km;
        } catch (Exception e) {
            throw new RuntimeException("SM4密钥生成失败", e);
        }
    }
}
