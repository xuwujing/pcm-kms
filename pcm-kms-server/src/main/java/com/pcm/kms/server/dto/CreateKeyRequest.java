package com.pcm.kms.server.dto;

import lombok.Data;

@Data
public class CreateKeyRequest {
    /** 别名 */
    private String alias;
    /** 算法：aes/sm4/rsa/sm2/sign/md5/sm3 */
    private String algorithm;
    /** 绑定的应用 Client ID（必填） */
    private String clientId;
    /** 描述 */
    private String description;
}
