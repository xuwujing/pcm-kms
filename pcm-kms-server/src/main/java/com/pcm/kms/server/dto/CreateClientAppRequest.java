package com.pcm.kms.server.dto;

import lombok.Data;

@Data
public class CreateClientAppRequest {
    private String clientName;
    private String clientGroup;
    private String contacts;
    private String mobile;
    private String jobNo;
}
