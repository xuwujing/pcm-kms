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
 * 记录所有密钥操作和 API 调用，不记录明文和完整密文。
 * operation 和 resource 使用中文名称，便于前端展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest request;

    /** 操作类型中文映射 */
    private static String op(String code) {
        switch (code) {
            case "app_create": return "创建应用";
            case "app_enable": return "启用应用";
            case "key_create": return "创建密钥";
            case "key_enable": return "启用密钥";
            case "key_disable": return "禁用密钥";
            case "key_rotate": return "轮转密钥";
            case "key_delete": return "删除密钥";
            case "auth_grant": return "授权密钥";
            case "auth_revoke": return "撤销授权";
            case "crypto_encrypt": return "加密";
            case "crypto_decrypt": return "解密";
            case "crypto_sign": return "签名";
            case "crypto_verify": return "验签";
            case "crypto_digest": return "摘要";
            case "user_create": return "创建用户";
            case "user_enable": return "启用用户";
            case "user_disable": return "禁用用户";
            case "user_reset_pwd": return "重置密码";
            case "login": return "登录";
            default: return code;
        }
    }

    /** 资源类型中文映射 */
    private static String res(String code) {
        switch (code) {
            case "ClientApp": return "应用";
            case "KeyMetadata": return "密钥";
            case "ClientKeyPermission": return "授权关系";
            case "User": return "用户";
            case "Digest": return "摘要";
            default: return code;
        }
    }

    public void log(String operation, String operator, String resource,
                    String resourceId, String result, String remark) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setOperation(op(operation));
            auditLog.setOperator(operator);
            auditLog.setResource(res(resource));
            auditLog.setResourceId(resourceId);
            auditLog.setResult("success".equals(result) ? "成功" : "失败");
            auditLog.setRemark(remark);
            try {
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                if ("0:0:0:0:0:0:0:1".equals(ip)) {
                    ip = "127.0.0.1";
                }
                auditLog.setIp(ip);
            } catch (Exception e) {
                auditLog.setIp("127.0.0.1");
            }
            auditLog.setCreatedAt(java.time.LocalDateTime.now());
            auditLogMapper.insert(auditLog);
            log.debug("审计日志已记录: op={}, resource={}, result={}", operation, resource, result);
        } catch (Exception e) {
            log.error("审计日志写入失败: op={}, error={}", operation, e.getMessage(), e);
        }
    }
}
