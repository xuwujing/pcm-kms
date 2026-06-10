package com.pcm.kms.server.service;

import com.pcm.kms.domain.model.AuditLog;
import com.pcm.kms.infra.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest request;

    /**
     * 记录审计日志
     */
    public void log(String operation, String operator, String resource, String resourceId, String result, String remark) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperation(operation);
        auditLog.setOperator(operator);
        auditLog.setResource(resource);
        auditLog.setResourceId(resourceId);
        auditLog.setResult(result);
        auditLog.setRemark(remark);
        try {
            auditLog.setIp(request.getRemoteAddr());
        } catch (Exception e) {
            auditLog.setIp("unknown");
        }
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }
}
