package com.pcm.kms.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.AppRateLimit;
import com.pcm.kms.domain.model.ClientApp;
import com.pcm.kms.infra.mapper.AppRateLimitMapper;
import com.pcm.kms.infra.mapper.ClientAppMapper;
import com.pcm.kms.server.config.KmsProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 限流配置控制器
 * <p>
 * 支持全局默认配置 + 每应用单独配置。
 * 应用未配置时使用全局默认值。
 */
@Slf4j
@Tag(name = "限流配置")
@RestController
@RequestMapping("/api/admin/ratelimit")
@RequiredArgsConstructor
public class RateLimitConfigController {

    private final KmsProperties kmsProperties;
    private final AppRateLimitMapper appRateLimitMapper;
    private final ClientAppMapper clientAppMapper;

    @GetMapping("/global")
    @Operation(summary = "获取全局限流配置")
    public ApiResponse<Map<String, Object>> getGlobalConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", kmsProperties.getRateLimit().isEnabled());
        config.put("maxPerMinute", kmsProperties.getRateLimit().getMaxPerMinute());
        return ApiResponse.success(config);
    }

    @PutMapping("/global")
    @Operation(summary = "更新全局限流配置")
    public ApiResponse<Map<String, Object>> updateGlobalConfig(@RequestBody RateLimitConfigRequest request) {
        log.info("更新全局限流配置: enabled={}, maxPerMinute={}", request.getEnabled(), request.getMaxPerMinute());
        if (request.getMaxPerMinute() != null) {
            kmsProperties.getRateLimit().setMaxPerMinute(request.getMaxPerMinute());
        }
        if (request.getEnabled() != null) {
            kmsProperties.getRateLimit().setEnabled(request.getEnabled());
        }
        return getGlobalConfig();
    }

    @GetMapping("/apps")
    @Operation(summary = "获取所有应用的限流配置")
    public ApiResponse<List<Map<String, Object>>> listAppConfigs() {
        List<ClientApp> apps = clientAppMapper.selectList(
                new LambdaQueryWrapper<ClientApp>().eq(ClientApp::getEnabled, true)
        );
        List<AppRateLimit> limits = appRateLimitMapper.selectList(null);

        Map<String, AppRateLimit> limitMap = new HashMap<>();
        for (AppRateLimit limit : limits) {
            limitMap.put(limit.getClientId(), limit);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ClientApp app : apps) {
            Map<String, Object> item = new HashMap<>();
            item.put("clientId", app.getClientId());
            item.put("clientName", app.getClientName());
            AppRateLimit limit = limitMap.get(app.getClientId());
            if (limit != null) {
                item.put("id", limit.getId());
                item.put("maxPerMinute", limit.getMaxPerMinute());
                item.put("enabled", limit.getEnabled());
                item.put("isCustom", true);
            } else {
                item.put("maxPerMinute", kmsProperties.getRateLimit().getMaxPerMinute());
                item.put("enabled", true);
                item.put("isCustom", false);
            }
            result.add(item);
        }
        return ApiResponse.success(result);
    }

    @PostMapping("/apps")
    @Operation(summary = "设置应用限流配置")
    public ApiResponse<Void> setAppConfig(@RequestBody AppRateLimitRequest request) {
        log.info("设置应用限流: clientId={}, maxPerMinute={}", request.getClientId(), request.getMaxPerMinute());

        AppRateLimit limit = appRateLimitMapper.selectOne(
                new LambdaQueryWrapper<AppRateLimit>().eq(AppRateLimit::getClientId, request.getClientId())
        );
        if (limit == null) {
            limit = new AppRateLimit();
            limit.setClientId(request.getClientId());
            limit.setMaxPerMinute(request.getMaxPerMinute());
            limit.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            limit.setCreatedAt(LocalDateTime.now());
            limit.setUpdatedAt(LocalDateTime.now());
            appRateLimitMapper.insert(limit);
        } else {
            if (request.getMaxPerMinute() != null) {
                limit.setMaxPerMinute(request.getMaxPerMinute());
            }
            if (request.getEnabled() != null) {
                limit.setEnabled(request.getEnabled());
            }
            limit.setUpdatedAt(LocalDateTime.now());
            appRateLimitMapper.updateById(limit);
        }
        return ApiResponse.success();
    }

    @DeleteMapping("/apps/{clientId}")
    @Operation(summary = "删除应用限流配置（恢复使用全局默认）")
    public ApiResponse<Void> deleteAppConfig(@PathVariable String clientId) {
        appRateLimitMapper.delete(
                new LambdaQueryWrapper<AppRateLimit>().eq(AppRateLimit::getClientId, clientId)
        );
        return ApiResponse.success();
    }

    @Data
    public static class RateLimitConfigRequest {
        private Boolean enabled;
        private Integer maxPerMinute;
    }

    @Data
    public static class AppRateLimitRequest {
        private String clientId;
        private Integer maxPerMinute;
        private Boolean enabled;
    }
}
