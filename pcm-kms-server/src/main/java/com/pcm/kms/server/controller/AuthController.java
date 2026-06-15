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
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null || !user.getEnabled()) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        String md5Password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!md5Password.equals(user.getPassword())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return ApiResponse.success(result);
    }

    @SaCheckLogin
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
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

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
