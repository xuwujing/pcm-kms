package com.pcm.kms.server.controller;

import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.CryptoResult;
import com.pcm.kms.server.dto.CryptoRequest;
import com.pcm.kms.server.service.CryptoBizService;
import com.pcm.kms.server.service.KeyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "加密服务")
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoBizService cryptoBizService;
    private final KeyService keyService;

    @PostMapping("/encrypt")
    @ApiOperation("加密")
    public ApiResponse<CryptoResult> encrypt(@RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.encrypt(request));
    }

    @PostMapping("/decrypt")
    @ApiOperation("解密")
    public ApiResponse<CryptoResult> decrypt(@RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.decrypt(request));
    }

    @PostMapping("/sign")
    @ApiOperation("签名")
    public ApiResponse<CryptoResult> sign(@RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.sign(request));
    }

    @PostMapping("/verify")
    @ApiOperation("验签")
    public ApiResponse<Boolean> verify(@RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.verify(request));
    }

    @PostMapping("/digest")
    @ApiOperation("摘要")
    public ApiResponse<CryptoResult> digest(@RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.digest(request));
    }

    @GetMapping("/public-key/{alias}")
    @ApiOperation("获取公钥")
    public ApiResponse<String> getPublicKey(@PathVariable String alias,
                                            @RequestParam(required = false) String clientGroup) {
        return ApiResponse.success(keyService.getPublicKey(alias, clientGroup));
    }
}
