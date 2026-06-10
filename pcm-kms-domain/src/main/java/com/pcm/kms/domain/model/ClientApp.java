package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户端应用（接入 KMS 的业务系统）
 */
@Data
public class ClientApp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 客户端唯一标识，启用后生成 */
    private String clientId;
    /** 客户端密钥，启用后生成 */
    private String clientSecret;
    /** 应用名称 */
    private String clientName;
    /** 应用组（隔离边界） */
    private String clientGroup;
    /** 联系人 */
    private String contacts;
    /** 手机号 */
    private String mobile;
    /** 工号 */
    private String jobNo;
    /** 是否启用 */
    private Boolean enabled;
    /** 签名公钥（启用后生成） */
    private String signPublicKey;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
