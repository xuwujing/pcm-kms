package com.pcm.kms.server.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.AuditLog;
import com.pcm.kms.infra.mapper.AuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@SaCheckLogin
@Tag(name = "审计日志")
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    @GetMapping
    @Operation(summary = "分页查询审计日志")
    public ApiResponse<IPage<AuditLog>> list(
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String operator,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getId);
        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(AuditLog::getOperation, operation);
        }
        if (operator != null && !operator.isEmpty()) {
            wrapper.eq(AuditLog::getOperator, operator);
        }
        return ApiResponse.success(auditLogMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
