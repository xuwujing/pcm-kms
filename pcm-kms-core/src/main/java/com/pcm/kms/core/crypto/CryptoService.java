package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.domain.model.CryptoResult;
import com.pcm.kms.domain.model.KeyMaterial;
import com.pcm.kms.domain.model.KeyMetadata;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 加密服务门面
 * 根据算法选择对应策略执行加解密
 */
@Service
public class CryptoService {

    private final Map<String, AlgorithmStrategy> strategyMap = new HashMap<>();

    public CryptoService(java.util.List<AlgorithmStrategy> strategies) {
        for (AlgorithmStrategy s : strategies) {
            strategyMap.put(s.getAlgorithm().getCode(), s);
        }
    }

    private AlgorithmStrategy resolve(AlgorithmEnum algorithm) {
        AlgorithmStrategy strategy = strategyMap.get(algorithm.getCode());
        if (strategy == null) {
            throw new KmsException(400, "算法暂未实现: " + algorithm.getCode());
        }
        return strategy;
    }

    /**
     * 加密
     */
    public CryptoResult encrypt(KeyMetadata metadata, KeyMaterial material, String plainText) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(metadata.getAlgorithm());
        String key = resolveKey(algorithm, material, true);
        String cipherText = resolve(algorithm).encrypt(plainText, key);
        CryptoResult result = new CryptoResult();
        result.setCipherText(cipherText);
        result.setAlgorithm(algorithm.getCode());
        result.setAlias(metadata.getAlias());
        result.setKeyVersion(metadata.getKeyVersion());
        return result;
    }

    /**
     * 解密
     */
    public CryptoResult decrypt(KeyMetadata metadata, KeyMaterial material, String cipherText) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(metadata.getAlgorithm());
        String key = resolveKey(algorithm, material, false);
        String plainText = resolve(algorithm).decrypt(cipherText, key);
        CryptoResult result = new CryptoResult();
        result.setPlainText(plainText);
        result.setAlgorithm(algorithm.getCode());
        result.setAlias(metadata.getAlias());
        result.setKeyVersion(metadata.getKeyVersion());
        return result;
    }

    /**
     * 签名
     */
    public CryptoResult sign(KeyMetadata metadata, KeyMaterial material, String data) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(metadata.getAlgorithm());
        String key = resolveKey(algorithm, material, false);
        String signature = resolve(algorithm).sign(data, key);
        CryptoResult result = new CryptoResult();
        result.setCipherText(signature);
        result.setAlgorithm(algorithm.getCode());
        result.setAlias(metadata.getAlias());
        result.setKeyVersion(metadata.getKeyVersion());
        return result;
    }

    /**
     * 验签
     */
    public boolean verify(KeyMetadata metadata, KeyMaterial material, String data, String signature) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(metadata.getAlgorithm());
        String key = resolveKey(algorithm, material, true);
        return resolve(algorithm).verify(data, signature, key);
    }

    /**
     * 摘要
     */
    public CryptoResult digest(AlgorithmEnum algorithm, String data) {
        String result = resolve(algorithm).digest(data);
        CryptoResult cr = new CryptoResult();
        cr.setCipherText(result);
        cr.setAlgorithm(algorithm.getCode());
        return cr;
    }

    /**
     * 生成密钥对
     */
    public KeyMaterial generateKeyMaterial(AlgorithmEnum algorithm) {
        AlgorithmStrategy strategy = resolve(algorithm);
        return strategy.generateKeyPair();
    }

    private String resolveKey(AlgorithmEnum algorithm, KeyMaterial material, boolean forEncrypt) {
        switch (algorithm.getCryptoType()) {
            case SYMMETRIC:
                return material.getSecretKey();
            case ASYMMETRIC:
                return forEncrypt ? material.getPublicKey() : material.getPrivateKey();
            case SIGN:
                return forEncrypt ? material.getPublicKey() : material.getPrivateKey();
            default:
                throw new KmsException(400, "无法解析密钥，加密类型: " + algorithm.getCryptoType());
        }
    }
}
