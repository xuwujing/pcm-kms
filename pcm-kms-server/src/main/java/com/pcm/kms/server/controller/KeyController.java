package com.pcm.kms.server.controller;

import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.KeyMetadata;
import com.pcm.kms.server.dto.CreateKeyRequest;
import com.pcm.kms.server.service.KeyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "密钥管理")
@RestController
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
public class KeyController {

    private final KeyService keyService;

    @PostMapping
    @ApiOperation("创建密钥")
    public ApiResponse<KeyMetadata> create(@RequestBody CreateKeyRequest request) {
        return ApiResponse.success(keyService.create(request));
    }

    @GetMapping
    @ApiOperation("密钥列表")
    public ApiResponse<List<KeyMetadata>> list(@RequestParam(required = false) String clientGroup) {
        return ApiResponse.success(keyService.list(clientGroup));
    }

    @GetMapping("/{id}")
    @ApiOperation("密钥详情")
    public ApiResponse<KeyMetadata> getById(@PathVariable Long id) {
        return ApiResponse.success(keyService.getById(id));
    }

    @PostMapping("/{id}/enable")
    @ApiOperation("启用密钥")
    public ApiResponse<KeyMetadata> enable(@PathVariable Long id) {
        return ApiResponse.success(keyService.toggleEnable(id, true));
    }

    @PostMapping("/{id}/disable")
    @ApiOperation("禁用密钥")
    public ApiResponse<KeyMetadata> disable(@PathVariable Long id) {
        return ApiResponse.success(keyService.toggleEnable(id, false));
    }

    @PostMapping("/{id}/rotate")
    @ApiOperation("密钥轮转")
    public ApiResponse<KeyMetadata> rotate(@PathVariable Long id) {
        return ApiResponse.success(keyService.rotate(id));
    }
}
