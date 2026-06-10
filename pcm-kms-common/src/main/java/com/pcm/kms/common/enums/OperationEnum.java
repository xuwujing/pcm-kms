package com.pcm.kms.common.enums;

/**
 * 操作类型枚举（审计日志用）
 */
public enum OperationEnum {

    APP_CREATE("app_create", "创建应用"),
    APP_ENABLE("app_enable", "启用应用"),
    APP_DISABLE("app_disable", "禁用应用"),
    KEY_CREATE("key_create", "创建密钥"),
    KEY_ENABLE("key_enable", "启用密钥"),
    KEY_DISABLE("key_disable", "禁用密钥"),
    KEY_ROTATE("key_rotate", "密钥轮转"),
    CRYPTO_ENCRYPT("crypto_encrypt", "加密"),
    CRYPTO_DECRYPT("crypto_decrypt", "解密"),
    CRYPTO_SIGN("crypto_sign", "签名"),
    CRYPTO_VERIFY("crypto_verify", "验签"),
    CRYPTO_DIGEST("crypto_digest", "摘要"),
    USER_LOGIN("user_login", "登录"),
    USER_LOGOUT("user_logout", "登出"),
    ;

    private final String code;
    private final String name;

    OperationEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}
