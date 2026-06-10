package com.pcm.kms.domain.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志
 */
@Data
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 操作类型 */
    private String operation;
    /** 操作人/clientId */
    private String operator;
    /** 目标资源 */
    private String resource;
    /** 资源ID */
    private String resourceId;
    /** 操作结果：success/failure */
    private String result;
    /** IP 地址 */
    private String ip;
    /** 备注 */
    private String remark;
    /** 操作时间 */
    private LocalDateTime createdAt;
}
