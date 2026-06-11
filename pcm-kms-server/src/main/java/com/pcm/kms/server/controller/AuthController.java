package com.pcm.kms.server.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.pcm.kms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 提供管理后台的登录/登出/用户信息接口，使用 Sa-Token 管理会话。
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 登录
     * <p>
     * 校验用户名密码，登录成功后返回 Token。当前版本使用硬编码账号（admin/123456），
     * 后续版本接入数据库用户表。
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());

        // 当前版本硬编码校验，后续接入 kms_user 表
        if ("admin".equals(request.getUsername()) && "123456".equals(request.getPassword())) {
            StpUtil.login(1L);
            log.info("登录成功: username={}, token={}", request.getUsername(), StpUtil.getTokenValue());
            Map<String, Object> result = new HashMap<>();
            result.put("token", StpUtil.getTokenValue());
            result.put("username", request.getUsername());
            return ApiResponse.success(result);
        }

        log.warn("登录失败: username={}, 原因=用户名或密码错误", request.getUsername());
        return ApiResponse.error(401, "用户名或密码错误");
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout() {
        log.info("登出请求: userId={}", StpUtil.getLoginIdDefaultNull());
        StpUtil.logout();
        return ApiResponse.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "当前用户信息")
    public ApiResponse<Map<String, Object>> info() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", loginId);
        result.put("username", "admin");
        result.put("nickname", "管理员");
        return ApiResponse.success(result);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
