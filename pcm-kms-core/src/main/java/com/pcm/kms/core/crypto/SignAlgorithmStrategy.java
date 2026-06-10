package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

/**
 * 签名策略（HMAC-SHA256 签名 + RSA 签名）
 */
@Component
public class SignAlgorithmStrategy implements AlgorithmStrategy {

    private static final String RSA_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SIGN;
    }

    @Override
    public String encrypt(String plainText, String key) {
        throw new UnsupportedOperationException("SIGN 不支持加密");
    }

    @Override
    public String decrypt(String cipherText, String key) {
        throw new UnsupportedOperationException("SIGN 不支持解密");
    }

    @Override
    public String sign(String data, String privateKeyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);
            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(RSA_ALGORITHM);
            java.security.PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new RuntimeException("签名失败", e);
        }
    }

    @Override
    public boolean verify(String data, String signStr, String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(RSA_ALGORITHM);
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signStr));
        } catch (Exception e) {
            throw new RuntimeException("验签失败", e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            KeyMaterial km = new KeyMaterial();
            km.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            km.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            return km;
        } catch (Exception e) {
            throw new RuntimeException("签名密钥对生成失败", e);
        }
    }
}
