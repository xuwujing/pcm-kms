package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户端密钥授权关系
 */
@Data
public class ClientKeyPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 客户端ID */
    private String clientId;
    /** 密钥ID */
    private String secretId;
    /** 是否启用 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
