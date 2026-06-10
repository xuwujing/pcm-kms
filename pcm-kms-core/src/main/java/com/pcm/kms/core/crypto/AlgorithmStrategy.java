package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.domain.model.KeyMaterial;

/**
 * 算法策略接口
 */
public interface AlgorithmStrategy {

    /** 返回支持的算法 */
    AlgorithmEnum getAlgorithm();

    /** 加密 */
    String encrypt(String plainText, String key);

    /** 解密 */
    String decrypt(String cipherText, String key);

    /** 签名（默认不支持） */
    default String sign(String data, String key) {
        throw new UnsupportedOperationException(getAlgorithm().getCode() + " 不支持签名");
    }

    /** 验签（默认不支持） */
    default boolean verify(String data, String signature, String key) {
        throw new UnsupportedOperationException(getAlgorithm().getCode() + " 不支持验签");
    }

    /** 摘要（默认不支持） */
    default String digest(String data) {
        throw new UnsupportedOperationException(getAlgorithm().getCode() + " 不支持摘要");
    }

    /** 生成密钥对/密钥材料 */
    KeyMaterial generateKeyPair();
}
