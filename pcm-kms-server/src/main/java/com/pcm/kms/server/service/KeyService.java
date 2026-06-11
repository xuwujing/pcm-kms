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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密钥管理服务
 * <p>
 * 负责密钥的创建、查询、启用/禁用、轮转。密钥材料通过 {@link CryptoService} 生成，
 * 私钥和对称密钥加密存储在 kms_key_material 表中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeyService {

    private final KeyMetadataMapper keyMetadataMapper;
    private final KeyMaterialMapper keyMaterialMapper;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;

    /**
     * 创建密钥
     * <p>
     * 根据指定算法生成密钥材料，创建元数据记录。别名在同一应用组内必须唯一。
     *
     * @param request 创建请求（别名、算法、应用组、描述）
     * @return 创建后的密钥元数据
     * @throws KmsException 别名重复或算法不支持时抛出
     */
    @Transactional
    public KeyMetadata create(CreateKeyRequest request) {
        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(request.getAlgorithm());
        String clientGroup = request.getClientGroup() != null ? request.getClientGroup() : "default";
        log.info("创建密钥: alias={}, algorithm={}, group={}", request.getAlias(), algorithm.getCode(), clientGroup);

        // 检查别名唯一性
        Long count = keyMetadataMapper.selectCount(
                new LambdaQueryWrapper<KeyMetadata>()
                        .eq(KeyMetadata::getAlias, request.getAlias())
                        .eq(KeyMetadata::getClientGroup, clientGroup)
        );
        if (count > 0) {
            log.warn("密钥创建失败: 别名已存在, alias={}, group={}", request.getAlias(), clientGroup);
            throw new KmsException(400, "别名已存在: " + request.getAlias());
        }

        // 生成密钥材料
        KeyMaterial material = cryptoService.generateKeyMaterial(algorithm);
        material.setSecretId(IdUtil.generateSecretId());
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(material);
        log.info("密钥材料已生成: secretId={}, algorithm={}", material.getSecretId(), algorithm.getCode());

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

        log.info("密钥创建成功: alias={}, secretId={}, version=1", request.getAlias(), material.getSecretId());
        auditLogService.log("key_create", "system", "KeyMetadata", metadata.getSecretId(), "success", null);
        return metadata;
    }

    /**
     * 查询密钥列表
     *
     * @param clientGroup 应用组（可选，为 null 时查询全部）
     * @return 密钥元数据列表，按创建时间倒序
     */
    public List<KeyMetadata> list(String clientGroup) {
        log.debug("查询密钥列表: group={}", clientGroup);
        LambdaQueryWrapper<KeyMetadata> wrapper = new LambdaQueryWrapper<>();
        if (clientGroup != null) {
            wrapper.eq(KeyMetadata::getClientGroup, clientGroup);
        }
        wrapper.orderByDesc(KeyMetadata::getCreatedAt);
        return keyMetadataMapper.selectList(wrapper);
    }

    /**
     * 根据 ID 查看密钥元数据
     *
     * @param id 密钥元数据ID
     * @return 密钥元数据，不存在返回 null
     */
    public KeyMetadata getById(Long id) {
        log.debug("查询密钥详情: id={}", id);
        return keyMetadataMapper.selectById(id);
    }

    /**
     * 按别名查询最新版本的启用密钥元数据
     * <p>
     * 加解密接口通过此方法定位密钥。返回指定别名下最新版本的启用密钥。
     *
     * @param alias       密钥别名
     * @param clientGroup 应用组（为 null 时使用 default）
     * @return 最新版本的启用密钥元数据，不存在返回 null
     */
    public KeyMetadata getByAlias(String alias, String clientGroup) {
        String group = clientGroup != null ? clientGroup : "default";
        log.debug("按别名查询密钥: alias={}, group={}", alias, group);
        return keyMetadataMapper.selectOne(
                new LambdaQueryWrapper<KeyMetadata>()
                        .eq(KeyMetadata::getAlias, alias)
                        .eq(KeyMetadata::getClientGroup, group)
                        .eq(KeyMetadata::getEnabled, true)
                        .orderByDesc(KeyMetadata::getKeyVersion)
                        .last("LIMIT 1")
        );
    }

    /**
     * 启用或禁用密钥
     *
     * @param id      密钥元数据ID
     * @param enabled true=启用 false=禁用
     * @return 更新后的密钥元数据
     * @throws KmsException 密钥不存在时抛出
     */
    public KeyMetadata toggleEnable(Long id, boolean enabled) {
        log.info("密钥状态变更: id={}, enabled={}", id, enabled);

        KeyMetadata metadata = keyMetadataMapper.selectById(id);
        if (metadata == null) {
            log.warn("密钥状态变更失败: 密钥不存在, id={}", id);
            throw new KmsException(404, "密钥不存在");
        }
        metadata.setEnabled(enabled);
        metadata.setUpdatedAt(LocalDateTime.now());
        keyMetadataMapper.updateById(metadata);

        log.info("密钥状态变更成功: alias={}, enabled={}", metadata.getAlias(), enabled);
        auditLogService.log(enabled ? "key_enable" : "key_disable", "system",
                "KeyMetadata", id.toString(), "success", null);
        return metadata;
    }

    /**
     * 密钥轮转
     * <p>
     * 生成新版本的密钥材料，创建新的元数据记录（版本号+1）。
     * 旧版本密钥保留，解密时仍可按版本号使用。
     *
     * @param id 要轮转的密钥元数据ID
     * @return 新版本的密钥元数据
     * @throws KmsException 密钥不存在时抛出
     */
    @Transactional
    public KeyMetadata rotate(Long id) {
        log.info("密钥轮转: id={}", id);

        KeyMetadata old = keyMetadataMapper.selectById(id);
        if (old == null) {
            log.warn("密钥轮转失败: 密钥不存在, id={}", id);
            throw new KmsException(404, "密钥不存在");
        }

        AlgorithmEnum algorithm = AlgorithmEnum.fromCode(old.getAlgorithm());
        log.info("轮转密钥: alias={}, 旧版本={}, algorithm={}", old.getAlias(), old.getKeyVersion(), algorithm.getCode());

        // 生成新密钥材料
        KeyMaterial newMaterial = cryptoService.generateKeyMaterial(algorithm);
        newMaterial.setSecretId(IdUtil.generateSecretId());
        newMaterial.setCreatedAt(LocalDateTime.now());
        newMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(newMaterial);
        log.info("轮转新密钥材料: secretId={}", newMaterial.getSecretId());

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

        log.info("密钥轮转成功: alias={}, 新版本={}, 新secretId={}",
                old.getAlias(), newMeta.getKeyVersion(), newMeta.getSecretId());
        auditLogService.log("key_rotate", "system", "KeyMetadata", newMeta.getSecretId(), "success",
                "从版本 " + old.getKeyVersion() + " 轮转到 " + newMeta.getKeyVersion());

        return newMeta;
    }

    /**
     * 根据 secretId 获取密钥材料
     *
     * @param secretId 密钥唯一标识
     * @return 密钥材料对象，不存在返回 null
     */
    public KeyMaterial getMaterialBySecretId(String secretId) {
        log.debug("查询密钥材料: secretId={}", secretId);
        return keyMaterialMapper.selectOne(
                new LambdaQueryWrapper<KeyMaterial>().eq(KeyMaterial::getSecretId, secretId)
        );
    }

    /**
     * 获取公钥（按别名）
     * <p>
     * 返回指定别名下最新版本密钥的公钥（PEM/Base64格式）。仅非对称算法密钥有公钥。
     *
     * @param alias       密钥别名
     * @param clientGroup 应用组（为 null 时使用 default）
     * @return 公钥字符串
     * @throws KmsException 密钥不存在或无公钥时抛出
     */
    public String getPublicKey(String alias, String clientGroup) {
        log.debug("获取公钥: alias={}, group={}", alias, clientGroup);

        KeyMetadata metadata = getByAlias(alias, clientGroup);
        if (metadata == null) {
            log.warn("获取公钥失败: 密钥不存在, alias={}", alias);
            throw new KmsException(404, "密钥不存在: " + alias);
        }
        KeyMaterial material = getMaterialBySecretId(metadata.getSecretId());
        if (material == null || material.getPublicKey() == null) {
            log.warn("获取公钥失败: 该密钥无公钥, alias={}, algorithm={}", alias, metadata.getAlgorithm());
            throw new KmsException(400, "该密钥没有公钥");
        }

        log.info("获取公钥成功: alias={}, version={}", alias, metadata.getKeyVersion());
        return material.getPublicKey();
    }
}
