package com.pcm.kms.server.controller;

import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.ClientApp;
import com.pcm.kms.server.dto.CreateClientAppRequest;
import com.pcm.kms.server.service.ClientAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "应用管理")
@RestController
@RequestMapping("/api/admin/apps")
@RequiredArgsConstructor
public class ClientAppController {

    private final ClientAppService clientAppService;

    @PostMapping
    @ApiOperation("创建应用")
    public ApiResponse<ClientApp> create(@RequestBody CreateClientAppRequest request) {
        return ApiResponse.success(clientAppService.create(request));
    }

    @GetMapping
    @ApiOperation("应用列表")
    public ApiResponse<List<ClientApp>> list() {
        return ApiResponse.success(clientAppService.list());
    }

    @GetMapping("/{id}")
    @ApiOperation("应用详情")
    public ApiResponse<ClientApp> getById(@PathVariable Long id) {
        return ApiResponse.success(clientAppService.getById(id));
    }

    @PostMapping("/{id}/enable")
    @ApiOperation("启用应用（生成接入凭证和默认密钥）")
    public ApiResponse<ClientApp> enable(@PathVariable Long id) {
        return ApiResponse.success(clientAppService.enable(id));
    }
}
