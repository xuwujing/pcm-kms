package com.pcm.kms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加密类型枚举
 */
@Getter
@AllArgsConstructor
public enum CryptoTypeEnum {

    SYMMETRIC(1, "symmetric", "对称加密"),
    ASYMMETRIC(2, "asymmetric", "非对称加密"),
    SIGN(3, "sign", "签名算法"),
    DIGESTER(4, "digester", "摘要算法"),
    ;

    private final Integer index;
    private final String code;
    private final String name;
}
