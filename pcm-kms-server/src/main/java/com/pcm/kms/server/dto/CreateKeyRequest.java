package com.pcm.kms.server.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建密钥请求体
 */
@Data
public class CreateKeyRequest {
    /** 别名（必填，同应用组内唯一） */
    @NotBlank(message = "别名不能为空")
    @Size(max = 128, message = "别名最长 128 个字符")
    private String alias;

    /** 算法：aes/sm4/rsa/sm2/sign/md5/sm3（必填） */
    @NotBlank(message = "算法不能为空")
    private String algorithm;

    /** 绑定的应用 Client ID（必填） */
    @NotBlank(message = "必须指定绑定的应用")
    private String clientId;

    /** 描述 */
    @Size(max = 512, message = "描述最长 512 个字符")
    private String description;
}
