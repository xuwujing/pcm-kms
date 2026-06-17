package com.pcm.kms.server.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
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
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理控制器
 * <p>
 * 提供用户 CRUD、启用/禁用、重置密码等管理操作。
 * 密码使用 BCrypt 哈希算法存储。
 */
@Slf4j
@SaCheckLogin
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    /** BCrypt 密码编码器（线程安全，可复用） */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @GetMapping
    @Operation(summary = "用户列表")
    public ApiResponse<List<User>> list() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getId)
        );
        // 脱敏：不返回密码字段
        users.forEach(u -> u.setPassword(null));
        return ApiResponse.success(users);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public ApiResponse<User> create(@Valid @RequestBody CreateUserRequest request) {
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existing != null) {
            return ApiResponse.error(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        log.info("创建用户成功: username={}", user.getUsername());
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PutMapping
    @Operation(summary = "编辑用户")
    public ApiResponse<User> update(@Valid @RequestBody UpdateUserRequest request) {
        User user = userMapper.selectById(request.getId());
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用或禁用用户")
    public ApiResponse<Void> enable(@PathVariable Long id, @RequestParam Boolean enabled) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        if ("admin".equals(user.getUsername()) && !enabled) {
            return ApiResponse.error(400, "不允许禁用管理员账号");
        }
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户状态变更: username={}, enabled={}", user.getUsername(), enabled);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码", description = "将密码重置为默认值 123456")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        user.setPassword(PASSWORD_ENCODER.encode("123456"));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("重置用户密码: username={}", user.getUsername());
        return ApiResponse.success();
    }

    /**
     * 创建用户请求体
     */
    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名长度 2-32 个字符")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度 6-64 个字符")
        private String password;

        @Size(max = 64, message = "昵称最长 64 个字符")
        private String nickname;
    }

    /**
     * 编辑用户请求体
     */
    @Data
    public static class UpdateUserRequest {
        private Long id;
        @Size(max = 64, message = "昵称最长 64 个字符")
        private String nickname;
    }
}
