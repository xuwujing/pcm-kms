package com.pcm.kms.server.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 加解密请求体
 * <p>
 * 不同操作使用不同字段：
 * <ul>
 *   <li>加密：plainText + alias + clientGroup</li>
 *   <li>解密：cipherText + alias + clientGroup + keyVersion（可选，指定旧版本密钥解密）</li>
 *   <li>签名：data + alias + clientGroup</li>
 *   <li>验签：data + signature + alias + clientGroup</li>
 *   <li>摘要：plainText + algorithm</li>
 * </ul>
 */
@Data
public class CryptoRequest {
    /** 明文（加密时使用） */
    @Size(max = 65536, message = "明文最长 65536 个字符")
    private String plainText;

    /** 密文（解密时使用） */
    @Size(max = 131072, message = "密文最长 131072 个字符")
    private String cipherText;

    /** 密钥别名（必填，摘要接口除外） */
    @Size(max = 128, message = "别名最长 128 个字符")
    private String alias;

    /** 算法（摘要时使用：md5/sm3） */
    private String algorithm;

    /** 应用组（默认 default） */
    @Size(max = 64, message = "应用组最长 64 个字符")
    private String clientGroup;

    /** 签名原文（签名/验签时使用） */
    @Size(max = 65536, message = "数据最长 65536 个字符")
    private String data;

    /** 签名值（验签时使用） */
    @Size(max = 8192, message = "签名值最长 8192 个字符")
    private String signature;

    /**
     * 密钥版本号（可选）
     * <p>
     * 解密时可指定旧版本号，用于密钥轮转后的过渡期解密。
     * 不指定时默认使用最新版本的启用密钥。
     */
    private Integer keyVersion;
}
