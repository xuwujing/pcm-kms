package com.pcm.kms.server.controller;

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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理控制器
 */
@Slf4j
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping
    @Operation(summary = "用户列表")
    public ApiResponse<List<User>> list() {
        return ApiResponse.success(userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByDesc(User::getId)
        ));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public ApiResponse<User> create(@RequestBody CreateUserRequest request) {
        log.info("创建用户: username={}", request.getUsername());

        // 检查用户名是否已存在
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existing != null) {
            return ApiResponse.error(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(md5(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        // 不返回密码
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @PutMapping
    @Operation(summary = "编辑用户")
    public ApiResponse<User> update(@RequestBody UpdateUserRequest request) {
        log.info("编辑用户: id={}", request.getId());
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
    @Operation(summary = "启用/禁用用户")
    public ApiResponse<Void> enable(@PathVariable Long id, @RequestParam Boolean enabled) {
        log.info("启用/禁用用户: id={}, enabled={}", id, enabled);
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        // 不允许禁用 admin
        if ("admin".equals(user.getUsername()) && !enabled) {
            return ApiResponse.error(400, "不允许禁用管理员账号");
        }
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置密码")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        log.info("重置密码: id={}", id);
        User user = userMapper.selectById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        user.setPassword(md5("123456"));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResponse.success();
    }

    private String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes());
    }

    @Data
    public static class CreateUserRequest {
        private String username;
        private String password;
        private String nickname;
    }

    @Data
    public static class UpdateUserRequest {
        private Long id;
        private String nickname;
    }
}
