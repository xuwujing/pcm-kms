package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * SM2 国密非对称加密策略
 * 使用 BouncyCastle 的 EC 密钥对 + SM2 签名/加密
 */
@Component
public class Sm2AlgorithmStrategy implements AlgorithmStrategy {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String ALGORITHM = "EC";
    private static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SM2;
    }

    @Override
    public String encrypt(String plainText, String publicKeyStr) {
        // SM2 加密：使用公钥加密
        try {
            byte[] pubKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("SM2", PROVIDER);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("SM2加密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String cipherText, String privateKeyStr) {
        try {
            byte[] priKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(priKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM, PROVIDER);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("SM2", PROVIDER);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("SM2解密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public KeyMaterial generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
            keyGen.initialize(256, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            KeyMaterial km = new KeyMaterial();
            km.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            km.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            return km;
        } catch (Exception e) {
            throw new RuntimeException("SM2密钥对生成失败: " + e.getMessage(), e);
        }
    }
}
