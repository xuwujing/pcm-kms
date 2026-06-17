package com.pcm.kms.server.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建应用请求体
 */
@Data
public class CreateClientAppRequest {
    /** 服务标识（clientId），可选，为空时启用应用时自动生成 */
    @Size(max = 64, message = "clientId 最长 64 个字符")
    private String clientId;

    /** 应用名称（必填） */
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 128, message = "应用名称最长 128 个字符")
    private String clientName;

    /** 应用组（可选，默认 default） */
    @Size(max = 64, message = "应用组最长 64 个字符")
    private String clientGroup;

    /** 联系人 */
    @Size(max = 64, message = "联系人最长 64 个字符")
    private String contacts;

    /** 手机号 */
    @Size(max = 32, message = "手机号最长 32 个字符")
    private String mobile;

    /** 工号 */
    @Size(max = 32, message = "工号最长 32 个字符")
    private String jobNo;

    /** 备注 */
    @Size(max = 512, message = "备注最长 512 个字符")
    private String remark;
}
