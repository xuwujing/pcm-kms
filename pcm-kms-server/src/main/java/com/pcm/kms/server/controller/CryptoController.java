package com.pcm.kms.server.controller;

import com.pcm.kms.common.response.ApiResponse;
import com.pcm.kms.domain.model.CryptoResult;
import com.pcm.kms.server.dto.CryptoRequest;
import com.pcm.kms.server.service.CryptoBizService;
import com.pcm.kms.server.service.KeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 加解密接口控制器
 * <p>
 * 提供加密、解密、签名、验签、摘要和公钥获取接口。
 * 通过 {@link com.pcm.kms.server.filter.SignatureVerifyInterceptor} 进行客户端签名校验。
 */
@Tag(name = "加密服务")
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoBizService cryptoBizService;
    private final KeyService keyService;

    @PostMapping("/encrypt")
    @Operation(summary = "加密", description = "使用指定别名的密钥加密明文，返回密文")
    public ApiResponse<CryptoResult> encrypt(@Valid @RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.encrypt(request));
    }

    @PostMapping("/decrypt")
    @Operation(summary = "解密", description = "使用指定别名的密钥解密密文，支持指定 keyVersion 使用旧版本密钥")
    public ApiResponse<CryptoResult> decrypt(@Valid @RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.decrypt(request));
    }

    @PostMapping("/sign")
    @Operation(summary = "签名", description = "使用指定别名的签名密钥对数据签名")
    public ApiResponse<CryptoResult> sign(@Valid @RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.sign(request));
    }

    @PostMapping("/verify")
    @Operation(summary = "验签", description = "使用指定别名的签名密钥验证签名是否正确")
    public ApiResponse<Boolean> verify(@Valid @RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.verify(request));
    }

    @PostMapping("/digest")
    @Operation(summary = "摘要", description = "计算数据的哈希摘要（MD5/SM3），不需要密钥")
    public ApiResponse<CryptoResult> digest(@Valid @RequestBody CryptoRequest request) {
        return ApiResponse.success(cryptoBizService.digest(request));
    }

    @GetMapping("/public-key/{alias}")
    @Operation(summary = "获取公钥", description = "获取指定别名下最新版本密钥的公钥（Base64 格式）")
    public ApiResponse<String> getPublicKey(@PathVariable String alias,
                                            @RequestParam(required = false) String clientGroup) {
        return ApiResponse.success(keyService.getPublicKey(alias, clientGroup));
    }
}
