package com.pcm.kms.server.dto;

import lombok.Data;

@Data
public class CreateClientAppRequest {
    /** 服务标识（clientId），如 kms、order-service，唯一 */
    private String clientId;
    /** 应用名称 */
    private String clientName;
    /** 应用组 */
    private String clientGroup;
    /** 联系人 */
    private String contacts;
    /** 手机号 */
    private String mobile;
    /** 工号 */
    private String jobNo;
}
