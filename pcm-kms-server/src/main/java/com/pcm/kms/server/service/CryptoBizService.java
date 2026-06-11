package com.pcm.kms.server.service;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.core.crypto.CryptoService;
import com.pcm.kms.domain.model.*;
import com.pcm.kms.server.dto.CryptoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 加解密业务服务
 * <p>
 * 对外暴露的加解密业务入口，负责密钥定位、调用加密引擎、记录审计日志。
 * 密钥定位通过 {@link KeyService#getByAlias} 实现别名到密钥材料的映射。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoBizService {

    private final KeyService keyService;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;

    /**
     * 加密
     *
     * @param request 加密请求（plainText + alias + clientGroup）
     * @return 加密结果（含 cipherText、algorithm、keyVersion）
     * @throws KmsException 密钥不存在时抛出
     */
    public CryptoResult encrypt(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        log.info("加密请求: alias={}, group={}", request.getAlias(), clientGroup);

        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            log.warn("加密失败: 密钥不存在, alias={}, group={}", request.getAlias(), clientGroup);
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());

        try {
            CryptoResult result = cryptoService.encrypt(metadata, material, request.getPlainText());
            log.info("加密成功: alias={}, algorithm={}, version={}", request.getAlias(), result.getAlgorithm(), result.getKeyVersion());
            auditLogService.log("crypto_encrypt", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
            return result;
        } catch (Exception e) {
            log.error("加密异常: alias={}, error={}", request.getAlias(), e.getMessage(), e);
            auditLogService.log("crypto_encrypt", "api", "KeyMetadata", metadata.getSecretId(), "failure", e.getMessage());
            throw e;
        }
    }

    /**
     * 解密
     *
     * @param request 解密请求（cipherText + alias + clientGroup）
     * @return 解密结果（含 plainText、algorithm、keyVersion）
     * @throws KmsException 密钥不存在时抛出
     */
    public CryptoResult decrypt(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        log.info("解密请求: alias={}, group={}", request.getAlias(), clientGroup);

        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            log.warn("解密失败: 密钥不存在, alias={}, group={}", request.getAlias(), clientGroup);
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());

        try {
            CryptoResult result = cryptoService.decrypt(metadata, material, request.getCipherText());
            log.info("解密成功: alias={}, algorithm={}, version={}", request.getAlias(), result.getAlgorithm(), result.getKeyVersion());
            auditLogService.log("crypto_decrypt", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
            return result;
        } catch (Exception e) {
            log.error("解密异常: alias={}, error={}", request.getAlias(), e.getMessage(), e);
            auditLogService.log("crypto_decrypt", "api", "KeyMetadata", metadata.getSecretId(), "failure", e.getMessage());
            throw e;
        }
    }

    /**
     * 签名
     *
     * @param request 签名请求（data + alias + clientGroup）
     * @return 签名结果（signature 在 cipherText 字段中）
     */
    public CryptoResult sign(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        log.info("签名请求: alias={}, group={}", request.getAlias(), clientGroup);

        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            log.warn("签名失败: 密钥不存在, alias={}, group={}", request.getAlias(), clientGroup);
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());

        try {
            CryptoResult result = cryptoService.sign(metadata, material, request.getData());
            log.info("签名成功: alias={}, version={}", request.getAlias(), result.getKeyVersion());
            auditLogService.log("crypto_sign", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
            return result;
        } catch (Exception e) {
            log.error("签名异常: alias={}, error={}", request.getAlias(), e.getMessage(), e);
            auditLogService.log("crypto_sign", "api", "KeyMetadata", metadata.getSecretId(), "failure", e.getMessage());
            throw e;
        }
    }

    /**
     * 验签
     *
     * @param request 验签请求（data + signature + alias + clientGroup）
     * @return 验签是否通过
     */
    public boolean verify(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        log.info("验签请求: alias={}, group={}", request.getAlias(), clientGroup);

        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            log.warn("验签失败: 密钥不存在, alias={}, group={}", request.getAlias(), clientGroup);
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());

        try {
            boolean result = cryptoService.verify(metadata, material, request.getData(), request.getSignature());
            log.info("验签结果: alias={}, pass={}", request.getAlias(), result);
            auditLogService.log("crypto_verify", "api", "KeyMetadata", metadata.getSecretId(),
                    result ? "success" : "failure", null);
            return result;
        } catch (Exception e) {
            log.error("验签异常: alias={}, error={}", request.getAlias(), e.getMessage(), e);
            auditLogService.log("crypto_verify", "api", "KeyMetadata", metadata.getSecretId(), "failure", e.getMessage());
            throw e;
        }
    }

    /**
     * 摘要计算（MD5/SM3）
     * <p>
     * 摘要算法不需要密钥，直接对输入数据计算哈希值。
     *
     * @param request 摘要请求（plainText + algorithm）
     * @return 摘要结果（hash 在 cipherText 字段中）
     */
    public CryptoResult digest(CryptoRequest request) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(request.getAlgorithm());
        log.info("摘要请求: algorithm={}", algorithm.getCode());

        try {
            CryptoResult result = cryptoService.digest(algorithm, request.getPlainText());
            log.info("摘要计算成功: algorithm={}", algorithm.getCode());
            auditLogService.log("crypto_digest", "api", "Digest", algorithm.getCode(), "success", null);
            return result;
        } catch (Exception e) {
            log.error("摘要异常: algorithm={}, error={}", algorithm.getCode(), e.getMessage(), e);
            throw e;
        }
    }
}
