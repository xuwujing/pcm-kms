package com.pcm.kms.server.service;

import com.pcm.kms.domain.model.AuditLog;
import com.pcm.kms.infra.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志服务
 * <p>
 * 记录所有密钥操作和 API 调用，包括操作类型、操作人、目标资源、结果等。
 * 不记录明文和完整密文，确保日志安全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest request;

    /**
     * 记录审计日志
     *
     * @param operation  操作类型（如 app_create、crypto_encrypt）
     * @param operator   操作人（管理员用户名或 clientId）
     * @param resource   资源类型（如 ClientApp、KeyMetadata）
     * @param resourceId 资源标识（ID 或 secretId）
     * @param result     操作结果（success / failure）
     * @param remark     备注信息
     */
    public void log(String operation, String operator, String resource,
                    String resourceId, String result, String remark) {
        try {
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
            auditLog.setCreatedAt(java.time.LocalDateTime.now());
            auditLogMapper.insert(auditLog);
            log.debug("审计日志已记录: op={}, resource={}, resourceId={}, result={}",
                    operation, resource, resourceId, result);
        } catch (Exception e) {
            // 审计日志写入失败不应影响主业务流程
            log.error("审计日志写入失败: op={}, resource={}, error={}", operation, resource, e.getMessage(), e);
        }
    }
}
