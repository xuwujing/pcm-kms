package com.pcm.kms.core.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 主密钥服务
 * <p>
 * 负责使用主密钥（Master Key）对私钥和对称密钥进行二次加密后落库，
 * 确保密钥材料在数据库中不以明文形式存储。
 * <p>
 * 主密钥来源：
 * - 优先从环境变量 {@code PCM_KMS_MASTER_KEY} 读取
 * - 未配置时使用内置默认密钥（仅适用于开发环境，生产环境必须配置！）
 * <p>
 * 加密方案：
 * - 算法：AES-256-CBC + PKCS5Padding
 * - IV：每次加密生成 16 字节随机 IV，拼接在密文前
 * - 密钥派生：对原始主密钥做 SHA-256 哈希，取 32 字节作为 AES-256 密钥
 * <p>
 * 存储格式：Base64( IV(16 bytes) || ciphertext )
 */
@Slf4j
@Component
public class MasterKeyService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    /** 内置默认主密钥（仅开发环境使用，生产环境务必通过环境变量覆盖） */
    private static final String DEFAULT_MASTER_KEY = "pcm-kms-default-master-key-change-me";

    /** 派生后的 32 字节 AES-256 密钥 */
    private final byte[] derivedKey;

    /** 标记是否使用默认密钥（用于启动时输出警告） */
    private final boolean usingDefaultKey;

    public MasterKeyService() {
        String masterKey = System.getenv("PCM_KMS_MASTER_KEY");
        if (masterKey == null || masterKey.trim().isEmpty()) {
            masterKey = DEFAULT_MASTER_KEY;
            usingDefaultKey = true;
            log.warn("==========================================================");
            log.warn("  未配置 PCM_KMS_MASTER_KEY 环境变量，使用内置默认主密钥！");
            log.warn("  生产环境请务必设置环境变量 PCM_KMS_MASTER_KEY");
            log.warn("==========================================================");
        } else {
            usingDefaultKey = false;
            log.info("已加载 PCM_KMS_MASTER_KEY 环境变量作为主密钥");
        }
        this.derivedKey = deriveKey(masterKey);
    }

    /**
     * 加密敏感密钥材料（私钥 / 对称密钥）
     * <p>
     * 使用 AES-256-CBC + 随机 IV 加密，返回 Base64 编码的 IV+密文。
     *
     * @param plainKey 明文密钥（Base64 编码的原始密钥材料）
     * @return 加密后的密文（Base64 编码，格式为 IV + 密文）
     */
    public String encrypt(String plainKey) {
        if (plainKey == null || plainKey.isEmpty()) {
            return plainKey;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(derivedKey, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainKey.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + 密文
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("主密钥加密失败", e);
            throw new RuntimeException("密钥材料加密失败", e);
        }
    }

    /**
     * 解密敏感密钥材料（私钥 / 对称密钥）
     * <p>
     * 从密文中提取前 16 字节作为 IV，剩余部分作为密文，使用 AES-256-CBC 解密。
     *
     * @param encryptedKey 加密后的密文（Base64 编码，格式为 IV + 密文）
     * @return 解密后的明文密钥（Base64 编码的原始密钥材料）
     */
    public String decrypt(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            return encryptedKey;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedKey);

            // 如果长度小于 IV_LENGTH，说明不是加密格式（兼容历史明文数据）
            if (combined.length <= IV_LENGTH) {
                log.warn("密钥材料长度异常，可能是未加密的历史数据，直接返回");
                return encryptedKey;
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec keySpec = new SecretKeySpec(derivedKey, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败可能是历史明文数据，尝试直接返回
            log.warn("主密钥解密失败，可能是未加密的历史数据: {}", e.getMessage());
            return encryptedKey;
        }
    }

    /**
     * 判断是否正在使用默认主密钥
     *
     * @return true 表示未配置环境变量，使用了内置默认密钥
     */
    public boolean isUsingDefaultKey() {
        return usingDefaultKey;
    }

    /**
     * 对原始主密钥做 SHA-256 哈希，派生出固定 32 字节的 AES-256 密钥
     *
     * @param masterKey 原始主密钥字符串
     * @return 32 字节的派生密钥
     */
    private byte[] deriveKey(String masterKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("主密钥派生失败", e);
        }
    }
}
