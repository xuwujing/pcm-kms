package com.pcm.kms.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.enums.KeySourceEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.common.util.IdUtil;
import com.pcm.kms.core.crypto.CryptoService;
import com.pcm.kms.domain.model.*;
import com.pcm.kms.infra.mapper.*;
import com.pcm.kms.server.dto.CreateClientAppRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端应用管理服务
 * <p>
 * 负责应用的创建、启用、查询。启用时自动生成接入凭证（clientId/clientSecret）
 * 和默认密钥（签名密钥 + AES 对称密钥），并自动授权。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAppService {

    private final ClientAppMapper clientAppMapper;
    private final KeyMetadataMapper keyMetadataMapper;
    private final KeyMaterialMapper keyMaterialMapper;
    private final ClientKeyPermissionMapper permissionMapper;
    private final CryptoService cryptoService;
    private final AuditLogService auditLogService;

    /**
     * 创建应用（未启用状态）
     * <p>
     * 仅登记应用信息，不生成接入凭证。需调用 {@link #enable(Long)} 启用后才能使用。
     *
     * @param request 创建请求（名称、应用组、联系人等）
     * @return 创建后的应用对象
     */
    @Transactional
    public ClientApp create(CreateClientAppRequest request) {
        log.info("创建应用: clientId={}, name={}", request.getClientId(), request.getClientName());

        if (request.getClientId() == null || request.getClientId().isEmpty()) {
            throw new KmsException(400, "服务标识(clientId)不能为空");
        }

        // 检查 clientId 唯一性
        Long count = clientAppMapper.selectCount(
                new LambdaQueryWrapper<ClientApp>().eq(ClientApp::getClientId, request.getClientId())
        );
        if (count > 0) {
            throw new KmsException(400, "服务标识已存在: " + request.getClientId());
        }

        ClientApp app = new ClientApp();
        app.setClientId(request.getClientId());
        app.setClientName(request.getClientName());
        app.setClientGroup(request.getClientGroup() != null ? request.getClientGroup() : "default");
        app.setContacts(request.getContacts());
        app.setMobile(request.getMobile());
        app.setJobNo(request.getJobNo());
        app.setEnabled(false);
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        clientAppMapper.insert(app);

        log.info("应用创建成功: id={}, clientId={}, name={}", app.getId(), app.getClientId(), app.getClientName());
        auditLogService.log("app_create", "system", "ClientApp", app.getId().toString(), "success",
                "服务标识: " + request.getClientId());
        return app;
    }

    /**
     * 启用应用
     * <p>
     * 执行以下操作：
     * 1. 生成 clientId / clientSecret 接入凭证
     * 2. 生成 RSA 签名密钥对（用于请求签名验签）
     * 3. 生成 AES 默认对称加密密钥
     * 4. 自动授权这两个密钥给该客户端
     *
     * @param id 应用ID
     * @return 启用后的应用对象（含 clientId/clientSecret）
     * @throws KmsException 应用不存在或已启用时抛出
     */
    @Transactional
    public ClientApp enable(Long id) {
        log.info("启用应用: id={}", id);

        ClientApp app = clientAppMapper.selectById(id);
        if (app == null) {
            log.warn("启用应用失败: 应用不存在, id={}", id);
            throw new KmsException(404, "应用不存在");
        }
        if (app.getEnabled()) {
            log.warn("启用应用失败: 应用已启用, id={}", id);
            throw new KmsException(400, "应用已启用");
        }

        // 1. 生成 clientSecret
        app.setClientSecret(IdUtil.generateClientSecret());
        log.info("生成接入凭证: clientId={}, secret=***", app.getClientId());

        // 2. 生成签名密钥对
        KeyMaterial signKeyMaterial = cryptoService.generateKeyMaterial(AlgorithmEnum.SIGN);
        signKeyMaterial.setSecretId(IdUtil.generateSecretId());
        signKeyMaterial.setCreatedAt(LocalDateTime.now());
        signKeyMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(signKeyMaterial);
        log.info("生成签名密钥: secretId={}", signKeyMaterial.getSecretId());

        app.setSignPublicKey(signKeyMaterial.getPublicKey());

        KeyMetadata signKeyMeta = buildKeyMetadata(app.getClientGroup(), signKeyMaterial.getSecretId(),
                AlgorithmEnum.SIGN, app.getClientId() + "_sign", "默认签名密钥", "sign");
        keyMetadataMapper.insert(signKeyMeta);

        // 3. 生成默认 AES 密钥
        KeyMaterial aesKeyMaterial = cryptoService.generateKeyMaterial(AlgorithmEnum.AES);
        aesKeyMaterial.setSecretId(IdUtil.generateSecretId());
        aesKeyMaterial.setCreatedAt(LocalDateTime.now());
        aesKeyMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(aesKeyMaterial);
        log.info("生成默认AES密钥: secretId={}", aesKeyMaterial.getSecretId());

        KeyMetadata aesKeyMeta = buildKeyMetadata(app.getClientGroup(), aesKeyMaterial.getSecretId(),
                AlgorithmEnum.AES, app.getClientId() + "_default", "默认对称加密密钥", "encrypt");
        keyMetadataMapper.insert(aesKeyMeta);

        // 4. 授权默认密钥
        grantPermission(app.getClientId(), signKeyMaterial.getSecretId());
        grantPermission(app.getClientId(), aesKeyMaterial.getSecretId());

        app.setEnabled(true);
        app.setUpdatedAt(LocalDateTime.now());
        clientAppMapper.updateById(app);

        log.info("应用启用成功: id={}, clientId={}", id, app.getClientId());
        auditLogService.log("app_enable", "system", "ClientApp", id.toString(), "success",
                "clientId: " + app.getClientId());
        return app;
    }

    /**
     * 启用/停用应用
     */
    @Transactional
    public ClientApp toggleEnable(Long id, boolean enabled) {
        log.info("启用/停用应用: id={}, enabled={}", id, enabled);
        ClientApp app = clientAppMapper.selectById(id);
        if (app == null) {
            throw new KmsException(404, "应用不存在");
        }
        if (enabled && !app.getEnabled()) {
            return enable(id); // 首次启用走完整流程
        }
        app.setEnabled(enabled);
        app.setUpdatedAt(LocalDateTime.now());
        clientAppMapper.updateById(app);
        auditLogService.log(enabled ? "app_enable" : "app_disable", "system", "ClientApp", id.toString(), "success", null);
        return app;
    }

    /**
     * 删除应用
     */
    @Transactional
    public void delete(Long id) {
        log.info("删除应用: id={}", id);
        ClientApp app = clientAppMapper.selectById(id);
        if (app == null) {
            throw new KmsException(404, "应用不存在");
        }
        clientAppMapper.deleteById(id);
        // 同时删除授权关系
        if (app.getClientId() != null) {
            permissionMapper.delete(
                    new LambdaQueryWrapper<ClientKeyPermission>().eq(ClientKeyPermission::getClientId, app.getClientId())
            );
        }
        auditLogService.log("app_delete", "system", "ClientApp", id.toString(), "success",
                "删除应用: " + app.getClientName());
    }

    /**
     * 分页查询应用列表
     */
    public IPage<ClientApp> listPage(Integer page, Integer size) {
        log.debug("分页查询应用列表: page={}, size={}", page, size);
        return clientAppMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ClientApp>().orderByDesc(ClientApp::getId));
    }

    /**
     * 查询所有应用列表
     *
     * @return 应用列表
     */
    public List<ClientApp> list() {
        log.debug("查询应用列表");
        return clientAppMapper.selectList(null);
    }

    /**
     * 根据 ID 查询应用
     *
     * @param id 应用ID
     * @return 应用对象，不存在返回 null
     */
    public ClientApp getById(Long id) {
        log.debug("查询应用详情: id={}", id);
        return clientAppMapper.selectById(id);
    }

    /**
     * 根据 clientId 查询应用
     *
     * @param clientId 客户端唯一标识
     * @return 应用对象，不存在返回 null
     */
    public ClientApp getByClientId(String clientId) {
        log.debug("根据clientId查询应用: clientId={}", clientId);
        return clientAppMapper.selectOne(
                new LambdaQueryWrapper<ClientApp>().eq(ClientApp::getClientId, clientId)
        );
    }

    /**
     * 授予客户端访问密钥的权限
     *
     * @param clientId 客户端ID
     * @param secretId 密钥ID
     */
    private void grantPermission(String clientId, String secretId) {
        ClientKeyPermission perm = new ClientKeyPermission();
        perm.setClientId(clientId);
        perm.setSecretId(secretId);
        perm.setEnabled(true);
        perm.setCreatedAt(LocalDateTime.now());
        permissionMapper.insert(perm);
        log.debug("授权: clientId={} -> secretId={}", clientId, secretId);
    }

    /**
     * 构建密钥元数据对象
     */
    private KeyMetadata buildKeyMetadata(String clientGroup, String secretId,
                                          AlgorithmEnum algorithm, String alias,
                                          String description, String keyPurpose) {
        KeyMetadata meta = new KeyMetadata();
        meta.setClientGroup(clientGroup);
        meta.setSecretId(secretId);
        meta.setEnabled(true);
        meta.setAlgorithm(algorithm.getCode());
        meta.setCryptoType(algorithm.getCryptoType().getCode());
        meta.setKeyPurpose(keyPurpose);
        meta.setAlias(alias);
        meta.setDescription(description);
        meta.setKeySource(KeySourceEnum.SYSTEM.getCode());
        meta.setKeyVersion(1);
        meta.setCreatedAt(LocalDateTime.now());
        meta.setUpdatedAt(LocalDateTime.now());
        meta.setCreator("system");
        return meta;
    }
}
