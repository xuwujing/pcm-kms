package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * SM4 国密对称加密策略
 * <p>
 * 使用 SM4/CBC/PKCS5Padding 模式，每次加密生成随机 IV 拼接在密文前。
 * 存储格式：Base64( IV(16 bytes) || ciphertext )
 */
@Component
public class Sm4AlgorithmStrategy implements AlgorithmStrategy {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String ALGORITHM = "SM4";
    /** 使用 CBC 模式 + PKCS5Padding，比 ECB 更安全（相同明文产生不同密文） */
    private static final String TRANSFORMATION = "SM4/CBC/PKCS5Padding";
    /** SM4 的 IV 长度与分组长度一致，为 16 字节 */
    private static final int IV_LENGTH = 16;

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SM4;
    }

    /**
     * SM4 加密（CBC 模式 + 随机 IV）
     * <p>
     * 生成 16 字节随机 IV，加密后将 IV 拼接在密文前，整体 Base64 编码。
     *
     * @param plainText 明文
     * @param key       Base64 编码的 SM4 对称密钥
     * @return Base64 编码的 IV + 密文
     */
    @Override
    public String encrypt(String plainText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM);
            // 生成随机 IV
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    /**
     * SM4 解密（CBC 模式 + 提取 IV）
     * <p>
     * 从密文中提取前 16 字节作为 IV，剩余部分作为密文进行解密。
     *
     * @param cipherText Base64 编码的 IV + 密文
     * @param key        Base64 编码的 SM4 对称密钥
     * @return 解密后的明文
     */
    @Override
    public String decrypt(String cipherText, String key) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(Base64.getDecoder().decode(key), ALGORITHM);
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 提取 IV 和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);

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
