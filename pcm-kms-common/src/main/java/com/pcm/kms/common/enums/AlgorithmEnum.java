package com.pcm.kms.common.enums;

import com.pcm.kms.common.exception.KmsException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 加密算法枚举
 */
@Getter
@AllArgsConstructor
public enum AlgorithmEnum {

    RSA(1, "rsa", "RSA", CryptoTypeEnum.ASYMMETRIC),
    SM2(2, "sm2", "SM2", CryptoTypeEnum.ASYMMETRIC),
    AES(11, "aes", "AES", CryptoTypeEnum.SYMMETRIC),
    SM4(12, "sm4", "SM4", CryptoTypeEnum.SYMMETRIC),
    SIGN(21, "sign", "签名算法", CryptoTypeEnum.SIGN),
    MD5(31, "md5", "MD5", CryptoTypeEnum.DIGESTER),
    SM3(32, "sm3", "SM3", CryptoTypeEnum.DIGESTER),
    ;

    private final Integer index;
    private final String code;
    private final String name;
    private final CryptoTypeEnum cryptoType;

    public static AlgorithmEnum fromCode(String code) {
        for (AlgorithmEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        throw new KmsException(400, "不支持的算法: " + code);
    }
}
