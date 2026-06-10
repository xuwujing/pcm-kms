package com.pcm.kms.server.dto;

import lombok.Data;

@Data
public class CryptoRequest {
    /** 明文（加密时使用） */
    private String plainText;
    /** 密文（解密时使用） */
    private String cipherText;
    /** 密钥别名 */
    private String alias;
    /** 算法（摘要时使用） */
    private String algorithm;
    /** 应用组（默认 default） */
    private String clientGroup;
    /** 签名原文（验签时使用） */
    private String data;
    /** 签名值（验签时使用） */
    private String signature;
}
