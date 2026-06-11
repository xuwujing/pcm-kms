package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用限流配置
 */
@Data
public class AppRateLimit implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 应用 Client ID */
    private String clientId;
    /** 每分钟最大请求数 */
    private Integer maxPerMinute;
    /** 是否启用限流 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
