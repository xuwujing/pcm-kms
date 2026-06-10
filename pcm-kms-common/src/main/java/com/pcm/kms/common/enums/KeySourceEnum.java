package com.pcm.kms.common.enums;

/**
 * 密钥来源枚举
 */
public enum KeySourceEnum {

    SYSTEM("system", "系统生成"),
    MANUAL("manual", "手动导入"),
    ;

    private final String code;
    private final String name;

    KeySourceEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}
