package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * RSA 非对称加密策略
 * <p>
 * 使用 RSA-2048 + PKCS1Padding。
 * 单次加密最大数据长度为 245 字节（2048/8 - 11）。
 * 超过此长度会报错，业务方应使用对称加密或混合加密方案。
 */
@Component
public class RsaAlgorithmStrategy implements AlgorithmStrategy {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    private static final int KEY_SIZE = 2048;
    /** RSA-2048 + PKCS1Padding 单次最大可加密字节数 */
    private static final int MAX_PLAINTEXT_BYTES = (KEY_SIZE / 8) - 11;

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.RSA;
    }

    @Override
    public String encrypt(String plainText, String publicKeyStr) {
        try {
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            if (plainBytes.length > MAX_PLAINTEXT_BYTES) {
                throw new IllegalArgumentException(
                        "RSA-2048 单次加密最大支持 " + MAX_PLAINTEXT_BYTES + " 字节，当前数据 " + plainBytes.length
                                + " 字节。建议使用对称加密（AES/SM4）或混合加密方案。");
            }
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(ALGORITHM);
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainBytes);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA加密失败", e);
        }
    }

    @Override
    public String decrypt(String cipherText, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(ALGORITHM);
            java.security.PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA解密失败", e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            KeyMaterial km = new KeyMaterial();
            km.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            km.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            return km;
        } catch (Exception e) {
            throw new RuntimeException("RSA密钥对生成失败", e);
        }
    }
}
