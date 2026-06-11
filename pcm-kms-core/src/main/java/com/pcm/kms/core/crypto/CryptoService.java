package com.pcm.kms.core.crypto;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.domain.model.CryptoResult;
import com.pcm.kms.domain.model.KeyMaterial;
import com.pcm.kms.domain.model.KeyMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加密服务门面
 * <p>
 * 根据算法类型选择对应的 {@link AlgorithmStrategy} 策略实现，
 * 提供统一的加密、解密、签名、验签、摘要、密钥生成接口。
 * <p>
 * 使用策略模式：每个算法实现一个 AlgorithmStrategy，通过 Spring 自动注入。
 * 新增算法只需添加策略实现类，无需修改本类。
 */
@Slf4j
@Service
public class CryptoService {

    /** 算法策略映射：key = 算法code（如 aes、rsa），value = 对应策略实现 */
    private final Map<String, AlgorithmStrategy> strategyMap = new HashMap<>();

    /**
     * 构造函数：自动收集所有 AlgorithmStrategy 实现
     *
     * @param strategies Spring 注入的策略实现列表
     */
    public CryptoService(List<AlgorithmStrategy> strategies) {
        for (AlgorithmStrategy s : strategies) {
            strategyMap.put(s.getAlgorithm().getCode(), s);
            log.info("注册加密策略: {} -> {}", s.getAlgorithm().getCode(), s.getClass().getSimpleName());
        }
        log.info("加密服务初始化完成，已注册 {} 种算法策略", strategyMap.size());
    }

    /**
     * 根据算法枚举定位策略实现
     *
     * @param algorithm 算法枚举
     * @return 对应的策略实现
     * @throws KmsException 算法未实现时抛出
     */
    private AlgorithmStrategy resolve(AlgorithmEnum algorithm) {
        AlgorithmStrategy strategy = strategyMap.get(algorithm.getCode());
        if (strategy == null) {
            log.error("算法策略未找到: algorithm={}", algorithm.getCode());
            throw new KmsException(400, "算法暂未实现: " + algorithm.getCode());
        }
        return strategy;
    }

    /**
     * 加密
     *
     * @param metadata  密钥元数据（含算法、别名、版本）
     * @param material  密钥材料（含公钥/私钥/对称密钥）
     * @param plainText 明文
     * @return 加密结果（含密文、算法、别名、版本）
     */
    public CryptoResult encrypt(KeyMetadata metadata, KeyMaterial material, String plainText) {
        log.debug("执行加密: alias={}, algorithm={}", metadata.getAlias(), metadata.getAlgorithm());
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
     *
     * @param metadata   密钥元数据
     * @param material   密钥材料
     * @param cipherText 密文
     * @return 解密结果（含明文、算法、别名、版本）
     */
    public CryptoResult decrypt(KeyMetadata metadata, KeyMaterial material, String cipherText) {
        log.debug("执行解密: alias={}, algorithm={}", metadata.getAlias(), metadata.getAlgorithm());
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
     *
     * @param metadata 密钥元数据
     * @param material 密钥材料（使用私钥签名）
     * @param data     待签名数据
     * @return 签名结果（签名值在 cipherText 字段中）
     */
    public CryptoResult sign(KeyMetadata metadata, KeyMaterial material, String data) {
        log.debug("执行签名: alias={}, algorithm={}", metadata.getAlias(), metadata.getAlgorithm());
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
     *
     * @param metadata  密钥元数据
     * @param material  密钥材料（使用公钥验签）
     * @param data      原始数据
     * @param signature 签名值
     * @return 验签是否通过
     */
    public boolean verify(KeyMetadata metadata, KeyMaterial material, String data, String signature) {
        log.debug("执行验签: alias={}, algorithm={}", metadata.getAlias(), metadata.getAlgorithm());
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(metadata.getAlgorithm());
        String key = resolveKey(algorithm, material, true);
        return resolve(algorithm).verify(data, signature, key);
    }

    /**
     * 摘要计算（MD5/SM3 等哈希算法，不需要密钥）
     *
     * @param algorithm 摘要算法
     * @param data      待计算数据
     * @return 摘要结果（哈希值在 cipherText 字段中）
     */
    public CryptoResult digest(AlgorithmEnum algorithm, String data) {
        log.debug("执行摘要: algorithm={}", algorithm.getCode());
        String result = resolve(algorithm).digest(data);
        CryptoResult cr = new CryptoResult();
        cr.setCipherText(result);
        cr.setAlgorithm(algorithm.getCode());
        return cr;
    }

    /**
     * 生成密钥对/密钥材料
     * <p>
     * 对称算法生成对称密钥，非对称算法生成公私钥对。
     *
     * @param algorithm 算法枚举
     * @return 密钥材料对象
     */
    public KeyMaterial generateKeyMaterial(AlgorithmEnum algorithm) {
        log.info("生成密钥材料: algorithm={}", algorithm.getCode());
        AlgorithmStrategy strategy = resolve(algorithm);
        KeyMaterial km = strategy.generateKeyPair();
        log.info("密钥材料生成成功: algorithm={}, hasPublicKey={}, hasPrivateKey={}, hasSecretKey={}",
                algorithm.getCode(),
                km.getPublicKey() != null,
                km.getPrivateKey() != null,
                km.getSecretKey() != null);
        return km;
    }

    /**
     * 根据算法类型和操作方向解析需要使用的密钥
     *
     * @param algorithm  算法枚举
     * @param material   密钥材料
     * @param forEncrypt true=加密/验签（使用公钥），false=解密/签名（使用私钥）
     * @return 密钥字符串（Base64 编码）
     */
    private String resolveKey(AlgorithmEnum algorithm, KeyMaterial material, boolean forEncrypt) {
        switch (algorithm.getCryptoType()) {
            case SYMMETRIC:
                return material.getSecretKey();
            case ASYMMETRIC:
                return forEncrypt ? material.getPublicKey() : material.getPrivateKey();
            case SIGN:
                return forEncrypt ? material.getPublicKey() : material.getPrivateKey();
            default:
                log.error("无法解析密钥: cryptoType={}", algorithm.getCryptoType());
                throw new KmsException(400, "无法解析密钥，加密类型: " + algorithm.getCryptoType());
        }
    }
}
