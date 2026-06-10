package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 加解密结果 DTO
 */
@Data
public class CryptoResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 密文（Base64） */
    private String cipherText;
    /** 明文 */
    private String plainText;
    /** 使用的算法 */
    private String algorithm;
    /** 使用的别名 */
    private String alias;
    /** 密钥版本 */
    private Integer keyVersion;
}
