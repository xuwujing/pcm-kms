package com.pcm.kms.demo;

import com.pcm.kms.starter.KmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 业务服务示例：展示在 Service 层如何使用 KmsClient
 * <p>
 * 实际业务中，加解密操作通常在 Service 层完成，Controller 层不直接调用 KMS。
 * 这个类演示了更贴近真实业务的用法。
 */
@Slf4j
@Service
public class DemoService {

    @Autowired
    private KmsClient kmsClient;

    /**
     * 加密敏感字段
     * <p>
     * 适用场景：用户注册、数据入库前加密
     *
     * @param alias 密钥别名，如 "user-phone-aes"
     * @param plainText 明文数据
     * @return 密文
     */
    public String encryptField(String alias, String plainText) {
        log.info("加密字段: alias={}", alias);
        KmsClient.CryptoResult result = kmsClient.encrypt(plainText, alias);
        return result.getCipherText();
    }

    /**
     * 解密敏感字段
     * <p>
     * 适用场景：数据查询后解密展示
     *
     * @param alias 密钥别名
     * @param cipherText 密文
     * @return 明文
     */
    public String decryptField(String alias, String cipherText) {
        log.info("解密字段: alias={}", alias);
        KmsClient.CryptoResult result = kmsClient.decrypt(cipherText, alias);
        return result.getPlainText();
    }

    /**
     * 数据签名
     * <p>
     * 适用场景：接口防篡改、数据完整性校验
     *
     * @param alias 签名密钥别名，如 "api-sign-rsa"
     * @param data 待签名数据
     * @return 签名值（Base64）
     */
    public String signData(String alias, String data) {
        log.info("数据签名: alias={}", alias);
        KmsClient.CryptoResult result = kmsClient.sign(data, alias);
        return result.getCipherText();
    }

    /**
     * 验证签名
     * <p>
     * 适用场景：验证请求数据是否被篡改
     *
     * @param alias 签名密钥别名
     * @param data 原始数据
     * @param signature 签名值
     * @return 是否有效
     */
    public boolean verifySignature(String alias, String data, String signature) {
        log.info("验证签名: alias={}", alias);
        return kmsClient.verify(data, signature, alias);
    }

    /**
     * 计算数据摘要
     * <p>
     * 适用场景：数据指纹、完整性校验（不需要密钥）
     *
     * @param plainText 原始数据
     * @param algorithm 算法：MD5 或 SM3
     * @return 摘要值
     */
    public String computeDigest(String plainText, String algorithm) {
        log.info("计算摘要: algorithm={}", algorithm);
        KmsClient.CryptoResult result = kmsClient.digest(plainText, algorithm);
        return result.getCipherText();
    }
}
