package com.pcm.kms.server.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.User;
import com.pcm.kms.infra.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 提供登录、登出、当前用户信息接口。
 * 密码使用 BCrypt 哈希算法存储（比 MD5 更安全，自带盐值）。
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    /** BCrypt 密码编码器（线程安全，可复用） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @PostMapping("/login")
    @Operation(summary = "登录", description = "用户名密码登录，成功后返回 Sa-Token")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null || !user.getEnabled()) {
            log.warn("登录失败: 用户不存在或已禁用, username={}", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }

        // BCrypt 密码校验（兼容旧版 MD5 密码：如果不是 BCrypt 格式则用 MD5 校验后自动迁移）
        if (!checkPassword(request.getPassword(), user)) {
            log.warn("登录失败: 密码错误, username={}", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("tokenName", "satoken");
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        log.info("用户登录成功: username={}", user.getUsername());
        return ApiResponse.success(result);
    }

    @SaCheckLogin
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        StpUtil.logout();
        log.info("用户登出: loginId={}", loginId);
        return ApiResponse.success();
    }

    @SaCheckLogin
    @GetMapping("/info")
    @Operation(summary = "当前用户信息")
    public ApiResponse<Map<String, Object>> info() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return ApiResponse.error(401, "未登录");
        }
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return ApiResponse.error(401, "用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return ApiResponse.success(result);
    }

    /**
     * 校验密码（兼容 BCrypt 和旧版 MD5）
     * <p>
     * BCrypt 格式的密码以 "$2a$" / "$2b$" 开头。
     * 如果检测到旧版 MD5 格式，校验通过后自动迁移为 BCrypt。
     */
    private boolean checkPassword(String rawPassword, User user) {
        String storedPassword = user.getPassword();
        if (storedPassword != null && (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$"))) {
            // BCrypt 格式
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        // 旧版 MD5 格式兼容
        String md5Password = org.springframework.util.DigestUtils.md5DigestAsHex(rawPassword.getBytes());
        if (md5Password.equals(storedPassword)) {
            // 自动迁移为 BCrypt
            user.setPassword(PASSWORD_ENCODER.encode(rawPassword));
            user.setUpdatedAt(java.time.LocalDateTime.now());
            userMapper.updateById(user);
            log.info("用户密码已从 MD5 自动迁移为 BCrypt: username={}", user.getUsername());
            return true;
        }
        return false;
    }

    /**
     * 登录请求体
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
