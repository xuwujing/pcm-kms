package com.pcm.kms.server.service;

import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.core.crypto.CryptoService;
import com.pcm.kms.domain.model.*;
import com.pcm.kms.server.dto.CryptoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CryptoBizService {

    private final KeyService keyService;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;

    /**
     * 加密
     */
    public CryptoResult encrypt(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());
        CryptoResult result = cryptoService.encrypt(metadata, material, request.getPlainText());
        auditLogService.log("crypto_encrypt", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
        return result;
    }

    /**
     * 解密
     */
    public CryptoResult decrypt(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());
        CryptoResult result = cryptoService.decrypt(metadata, material, request.getCipherText());
        auditLogService.log("crypto_decrypt", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
        return result;
    }

    /**
     * 签名
     */
    public CryptoResult sign(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());
        CryptoResult result = cryptoService.sign(metadata, material, request.getData());
        auditLogService.log("crypto_sign", "api", "KeyMetadata", metadata.getSecretId(), "success", null);
        return result;
    }

    /**
     * 验签
     */
    public boolean verify(CryptoRequest request) {
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        KeyMetadata metadata = keyService.getByAlias(request.getAlias(), clientGroup);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在: " + request.getAlias());
        }
        KeyMaterial material = keyService.getMaterialBySecretId(metadata.getSecretId());
        return cryptoService.verify(metadata, material, request.getData(), request.getSignature());
    }

    /**
     * 摘要
     */
    public CryptoResult digest(CryptoRequest request) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(request.getAlgorithm());
        CryptoResult result = cryptoService.digest(algorithm, request.getPlainText());
        auditLogService.log("crypto_digest", "api", "Digest", algorithm.getCode(), "success", null);
        return result;
    }
}
