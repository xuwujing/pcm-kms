package com.pcm.kms.server.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.ClientKeyPermission;
import com.pcm.kms.domain.model.KeyMetadata;
import com.pcm.kms.server.dto.CreateKeyRequest;
import com.pcm.kms.server.service.KeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@SaCheckLogin
@Tag(name = "密钥管理")
@RestController
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
public class KeyController {

    private final KeyService keyService;

    @PostMapping
    @Operation(summary = "创建密钥")
    public ApiResponse<KeyMetadata> create(@Valid @RequestBody CreateKeyRequest request) {
        return ApiResponse.success(keyService.create(request));
    }

    @GetMapping
    @Operation(summary = "密钥列表")
    public ApiResponse<IPage<KeyMetadata>> list(
            @RequestParam(required = false) String clientGroup,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(keyService.listPage(clientGroup, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "密钥详情")
    public ApiResponse<KeyMetadata> getById(@PathVariable Long id) {
        return ApiResponse.success(keyService.getById(id));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用密钥")
    public ApiResponse<KeyMetadata> enable(@PathVariable Long id) {
        return ApiResponse.success(keyService.toggleEnable(id, true));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用密钥")
    public ApiResponse<KeyMetadata> disable(@PathVariable Long id) {
        return ApiResponse.success(keyService.toggleEnable(id, false));
    }

    @PostMapping("/{id}/rotate")
    @Operation(summary = "轮转密钥")
    public ApiResponse<KeyMetadata> rotate(@PathVariable Long id) {
        return ApiResponse.success(keyService.rotate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除密钥")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        keyService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/permissions")
    @Operation(summary = "查询密钥授权")
    public ApiResponse<List<ClientKeyPermission>> listPermissions(@PathVariable Long id) {
        KeyMetadata key = keyService.getById(id);
        if (key == null) {
            return ApiResponse.error(404, "密钥不存在");
        }
        return ApiResponse.success(keyService.listPermissions(key.getSecretId()));
    }

    @PostMapping("/{id}/grant")
    @Operation(summary = "授权应用访问密钥")
    public ApiResponse<Void> grantPermission(@PathVariable Long id, @RequestBody Map<String, String> body) {
        KeyMetadata key = keyService.getById(id);
        if (key == null) {
            return ApiResponse.error(404, "密钥不存在");
        }
        keyService.grantPermission(body.get("clientId"), key.getSecretId());
        return ApiResponse.success();
    }

    @DeleteMapping("/permission/{permissionId}")
    @Operation(summary = "撤销密钥授权")
    public ApiResponse<Void> revokePermission(@PathVariable Long permissionId) {
        keyService.revokePermission(permissionId);
        return ApiResponse.success();
    }
}
