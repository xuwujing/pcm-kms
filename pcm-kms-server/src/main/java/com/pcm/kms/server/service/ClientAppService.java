package com.pcm.kms.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcm.kms.common.enums.AlgorithmEnum;
import com.pcm.kms.common.enums.KeySourceEnum;
import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.common.util.IdUtil;
import com.pcm.kms.core.crypto.CryptoService;
import com.pcm.kms.domain.model.*;
import com.pcm.kms.infra.mapper.*;
import com.pcm.kms.server.dto.CreateClientAppRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
     */
    @Transactional
    public ClientApp create(CreateClientAppRequest request) {
        ClientApp app = new ClientApp();
        app.setClientName(request.getClientName());
        app.setClientGroup(request.getClientGroup() != null ? request.getClientGroup() : "default");
        app.setContacts(request.getContacts());
        app.setMobile(request.getMobile());
        app.setJobNo(request.getJobNo());
        app.setEnabled(false);
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        clientAppMapper.insert(app);
        auditLogService.log("app_create", "system", "ClientApp", app.getId().toString(), "success", null);
        return app;
    }

    /**
     * 启用应用：生成 clientId/clientSecret/默认签名密钥
     */
    @Transactional
    public ClientApp enable(Long id) {
        ClientApp app = clientAppMapper.selectById(id);
        if (app == null) {
            throw new KmsException(404, "应用不存在");
        }
        if (app.getEnabled()) {
            throw new KmsException(400, "应用已启用");
        }

        // 生成接入凭证
        app.setClientId(IdUtil.generateClientId());
        app.setClientSecret(IdUtil.generateClientSecret());

        // 生成默认签名密钥对
        KeyMaterial signKeyMaterial = cryptoService.generateKeyMaterial(AlgorithmEnum.SIGN);
        signKeyMaterial.setSecretId(IdUtil.generateSecretId());
        signKeyMaterial.setCreatedAt(LocalDateTime.now());
        signKeyMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(signKeyMaterial);

        // 保存签名公钥到 ClientApp
        app.setSignPublicKey(signKeyMaterial.getPublicKey());

        // 创建签名密钥元数据
        KeyMetadata signKeyMeta = new KeyMetadata();
        signKeyMeta.setClientGroup(app.getClientGroup());
        signKeyMeta.setSecretId(signKeyMaterial.getSecretId());
        signKeyMeta.setEnabled(true);
        signKeyMeta.setAlgorithm(AlgorithmEnum.SIGN.getCode());
        signKeyMeta.setCryptoType(AlgorithmEnum.SIGN.getCryptoType().getCode());
        signKeyMeta.setKeyPurpose("sign");
        signKeyMeta.setAlias(app.getClientId() + "_sign");
        signKeyMeta.setDescription("默认签名密钥");
        signKeyMeta.setKeySource(KeySourceEnum.SYSTEM.getCode());
        signKeyMeta.setKeyVersion(1);
        signKeyMeta.setCreatedAt(LocalDateTime.now());
        signKeyMeta.setUpdatedAt(LocalDateTime.now());
        signKeyMeta.setCreator("system");
        keyMetadataMapper.insert(signKeyMeta);

        // 生成默认 AES 密钥
        KeyMaterial aesKeyMaterial = cryptoService.generateKeyMaterial(AlgorithmEnum.AES);
        aesKeyMaterial.setSecretId(IdUtil.generateSecretId());
        aesKeyMaterial.setCreatedAt(LocalDateTime.now());
        aesKeyMaterial.setUpdatedAt(LocalDateTime.now());
        keyMaterialMapper.insert(aesKeyMaterial);

        KeyMetadata aesKeyMeta = new KeyMetadata();
        aesKeyMeta.setClientGroup(app.getClientGroup());
        aesKeyMeta.setSecretId(aesKeyMaterial.getSecretId());
        aesKeyMeta.setEnabled(true);
        aesKeyMeta.setAlgorithm(AlgorithmEnum.AES.getCode());
        aesKeyMeta.setCryptoType(AlgorithmEnum.AES.getCryptoType().getCode());
        aesKeyMeta.setKeyPurpose("encrypt");
        aesKeyMeta.setAlias(app.getClientId() + "_default");
        aesKeyMeta.setDescription("默认对称加密密钥");
        aesKeyMeta.setKeySource(KeySourceEnum.SYSTEM.getCode());
        aesKeyMeta.setKeyVersion(1);
        aesKeyMeta.setCreatedAt(LocalDateTime.now());
        aesKeyMeta.setUpdatedAt(LocalDateTime.now());
        aesKeyMeta.setCreator("system");
        keyMetadataMapper.insert(aesKeyMeta);

        // 授权默认密钥给该客户端
        grantPermission(app.getClientId(), signKeyMaterial.getSecretId());
        grantPermission(app.getClientId(), aesKeyMaterial.getSecretId());

        app.setEnabled(true);
        app.setUpdatedAt(LocalDateTime.now());
        clientAppMapper.updateById(app);

        auditLogService.log("app_enable", "system", "ClientApp", id.toString(), "success", null);
        return app;
    }

    /**
     * 列表查询
     */
    public List<ClientApp> list() {
        return clientAppMapper.selectList(null);
    }

    /**
     * 根据 ID 查询
     */
    public ClientApp getById(Long id) {
        return clientAppMapper.selectById(id);
    }

    /**
     * 根据 clientId 查询
     */
    public ClientApp getByClientId(String clientId) {
        return clientAppMapper.selectOne(
                new LambdaQueryWrapper<ClientApp>().eq(ClientApp::getClientId, clientId)
        );
    }

    private void grantPermission(String clientId, String secretId) {
        ClientKeyPermission perm = new ClientKeyPermission();
        perm.setClientId(clientId);
        perm.setSecretId(secretId);
        perm.setEnabled(true);
        perm.setCreatedAt(LocalDateTime.now());
        permissionMapper.insert(perm);
    }
}
