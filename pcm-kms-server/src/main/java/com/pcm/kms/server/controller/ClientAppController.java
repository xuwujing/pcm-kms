package com.pcm.kms.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.ClientApp;
import com.pcm.kms.server.dto.CreateClientAppRequest;
import com.pcm.kms.server.service.ClientAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "应用管理")
@RestController
@RequestMapping("/api/admin/apps")
@RequiredArgsConstructor
public class ClientAppController {

    private final ClientAppService clientAppService;

    @PostMapping
    @Operation(summary = "创建应用")
    public ApiResponse<ClientApp> create(@RequestBody CreateClientAppRequest request) {
        return ApiResponse.success(clientAppService.create(request));
    }

    @GetMapping
    @Operation(summary = "应用列表（分页）")
    public ApiResponse<IPage<ClientApp>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(clientAppService.listPage(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "应用详情")
    public ApiResponse<ClientApp> getById(@PathVariable Long id) {
        return ApiResponse.success(clientAppService.getById(id));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用应用")
    public ApiResponse<ClientApp> enable(@PathVariable Long id) {
        return ApiResponse.success(clientAppService.toggleEnable(id, true));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用应用")
    public ApiResponse<ClientApp> disable(@PathVariable Long id) {
        return ApiResponse.success(clientAppService.toggleEnable(id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除应用")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        clientAppService.delete(id);
        return ApiResponse.success();
    }
}
