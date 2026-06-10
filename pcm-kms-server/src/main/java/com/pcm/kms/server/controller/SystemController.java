package com.pcm.kms.server.controller;

import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.core.service.RuntimeInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "系统")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final RuntimeInfoService runtimeInfoService;

    @GetMapping("/ping")
    @ApiOperation("探活")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(runtimeInfoService.summary());
    }
}
