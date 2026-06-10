package com.pcm.kms.common.enums;

/**
 * 密钥用途枚举
 */
public enum KeyPurposeEnum {

    ENCRYPT("encrypt", "加密"),
    DECRYPT("decrypt", "解密"),
    SIGN("sign", "签名"),
    VERIFY("verify", "验签"),
    DIGEST("digest", "摘要"),
    ;

    private final String code;
    private final String name;

    KeyPurposeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}
