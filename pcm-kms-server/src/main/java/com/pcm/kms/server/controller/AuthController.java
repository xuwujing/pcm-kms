package com.pcm.kms.server.controller;

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
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());

        // 查询数据库用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null) {
            log.warn("登录失败: username={}, 原因=用户不存在", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }
        if (!user.getEnabled()) {
            log.warn("登录失败: username={}, 原因=用户已禁用", request.getUsername());
            return ApiResponse.error(401, "用户已禁用");
        }

        String md5Password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!md5Password.equals(user.getPassword())) {
            log.warn("登录失败: username={}, 原因=密码错误", request.getUsername());
            return ApiResponse.error(401, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        log.info("登录成功: username={}, token={}", request.getUsername(), StpUtil.getTokenValue());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return ApiResponse.success(result);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout() {
        log.info("登出请求: userId={}", StpUtil.getLoginIdDefaultNull());
        StpUtil.logout();
        return ApiResponse.success();
    }

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

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
