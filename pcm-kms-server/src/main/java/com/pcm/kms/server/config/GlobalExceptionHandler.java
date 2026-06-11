package com.pcm.kms.server.config;

import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获业务异常和系统异常，转换为标准 ApiResponse 格式返回。
 * 业务异常记录 WARN 级别日志，系统异常记录 ERROR 级别日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 KMS 业务异常
     * <p>
     * 业务异常通常是参数校验、资源不存在等可预期错误，不影响系统稳定性。
     */
    @ExceptionHandler(KmsException.class)
    public ApiResponse<?> handleKmsException(KmsException e) {
        log.warn("KMS业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理未知系统异常
     * <p>
     * 未预期的运行时异常，需要关注和排查。记录完整堆栈。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统内部异常: {}", e.getMessage(), e);
        return ApiResponse.error(500, "系统内部错误: " + e.getMessage());
    }
}
