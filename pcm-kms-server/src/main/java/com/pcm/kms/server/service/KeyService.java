package com.pcm.kms.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.enums.KeySourceEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.common.util.IdUtil;
import com.pcm.kms.core.crypto.CryptoService;
import com.pcm.kms.domain.model.*;
import com.pcm.kms.infra.mapper.KeyMaterialMapper;
import com.pcm.kms.infra.mapper.KeyMetadataMapper;
import com.pcm.kms.server.dto.CreateKeyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KeyService {

    private final KeyMetadataMapper keyMetadataMapper;
    private final KeyMaterialMapper keyMaterialMapper;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;

    /**
     * 创建密钥
     */
    @Transactional
    public KeyMetadata create(CreateKeyRequest request) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(request.getAlgorithm());
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";

        // 检查别名唯一性
        Long count = keyMetadataMapper.selectCount(
                new LambdaQueryWrapper<KeyMetadata>()
                        .eq(KeyMetadata::getAlias, request.getAlias())
                        .eq(KeyMetadata::getClientGroup, clientGroup)
        );
        if (count > 0) {
            throw new KmsException(400, "别名已存在: " + request.getAlias());
        }

        // 生成密钥材料
        KeyMaterial material = cryptoService.generateKeyMaterial(algorithm);
        material.setSecretId(IdUtil.generateSecretId());
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(material);

        // 创建元数据
        KeyMetadata metadata = new KeyMetadata();
        metadata.setClientGroup(clientGroup);
        metadata.setSecretId(material.getSecretId());
        metadata.setEnabled(true);
        metadata.setAlgorithm(algorithm.getCode());
        metadata.setCryptoType(algorithm.getCryptoType().getCode());
        metadata.setKeyPurpose("encrypt");
        metadata.setAlias(request.getAlias());
        metadata.setDescription(request.getDescription());
        metadata.setKeySource(KeySourceEnum.SYSTEM.getCode());
        metadata.setKeyVersion(1);
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setUpdatedAt(LocalDateTime.now());
        metadata.setCreator("system");
        keyMetadataMapper.insert(metadata);

        auditLogService.log("key_create", "system", "KeyMetadata", metadata.getSecretId(), "success", null);
        return metadata;
    }

    /**
     * 查询密钥列表
     */
    public List<KeyMetadata> list(String clientGroup) {
        LambdaQueryWrapper<KeyMetadata> wrapper = new LambdaQueryWrapper<>();
        if (clientGroup != null) {
            wrapper.eq(KeyMetadata::getClientGroup, clientGroup);
        }
        wrapper.orderByDesc(KeyMetadata::getCreatedAt);
        return keyMetadataMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查看密钥元数据
     */
    public KeyMetadata getById(Long id) {
        return keyMetadataMapper.selectById(id);
    }

    /**
     * 按别名查询最新版本密钥元数据
     */
    public KeyMetadata getByAlias(String alias, String clientGroup) {
        return keyMetadataMapper.selectOne(
                new LambdaQueryWrapper<KeyMetadata>()
                        .eq(KeyMetadata::getAlias, alias)
                        .eq(KeyMetadata::getClientGroup, clientGroup != null ? clientGroup : "default")
                        .eq(KeyMetadata::getEnabled, true)
                        .orderByDesc(KeyMetadata::getKeyVersion)
                        .last("LIMIT 1")
        );
    }

    /**
     * 启用/禁用密钥
     */
    public KeyMetadata toggleEnable(Long id, boolean enabled) {
        KeyMetadata metadata = keyMetadataMapper.selectById(id);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在");
        }
        metadata.setEnabled(enabled);
        metadata.setUpdatedAt(LocalDateTime.now());
        keyMetadataMapper.updateById(metadata);
        auditLogService.log(enabled ? "key_enable" : "key_disable", "system", "KeyMetadata", id.toString(), "success", null);
        return metadata;
    }

    /**
     * 密钥轮转：创建新版本
     */
    @Transactional
    public KeyMetadata rotate(Long id) {
        KeyMetadata old = keyMetadataMapper.selectById(id);
        if (old == null) {
            throw new KmsException(404, "密钥不存在");
        }

        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(old.getAlgorithm());

        // 生成新密钥材料
        KeyMaterial newMaterial = cryptoService.generateKeyMaterial(algorithm);
        newMaterial.setSecretId(IdUtil.generateSecretId());
        newMaterial.setCreatedAt(LocalDateTime.now());
        newMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(newMaterial);

        // 创建新版本元数据
        KeyMetadata newMeta = new KeyMetadata();
        newMeta.setClientGroup(old.getClientGroup());
        newMeta.setSecretId(newMaterial.getSecretId());
        newMeta.setEnabled(true);
        newMeta.setAlgorithm(old.getAlgorithm());
        newMeta.setCryptoType(old.getCryptoType());
        newMeta.setKeyPurpose(old.getKeyPurpose());
        newMeta.setAlias(old.getAlias());
        newMeta.setDescription(old.getDescription());
        newMeta.setKeySource(KeySourceEnum.SYSTEM.getCode());
        newMeta.setKeyVersion(old.getKeyVersion() + 1);
        newMeta.setCreatedAt(LocalDateTime.now());
        newMeta.setUpdatedAt(LocalDateTime.now());
        newMeta.setCreator("system");
        keyMetadataMapper.insert(newMeta);

        auditLogService.log("key_rotate", "system", "KeyMetadata", newMeta.getSecretId(), "success",
                "从版本 " + old.getKeyVersion() + " 轮转到 " + newMeta.getKeyVersion());

        return newMeta;
    }

    /**
     * 获取密钥材料
     */
    public KeyMaterial getMaterialBySecretId(String secretId) {
        return keyMaterialMapper.selectOne(
                new LambdaQueryWrapper<KeyMaterial>().eq(KeyMaterial::getSecretId, secretId)
        );
    }

    /**
     * 获取公钥（按别名）
     */
    public String getPublicKey(String alias, String clientGroup) {
        KeyMetadata metadata = getByAlias(alias, clientGroup);
        if (metadata == null) {
            throw new KmsException(404, "密钥不存在: " + alias);
        }
        KeyMaterial material = getMaterialBySecretId(metadata.getSecretId());
        if (material == null || material.getPublicKey() == null) {
            throw new KmsException(400, "该密钥没有公钥");
        }
        return material.getPublicKey();
    }
}
