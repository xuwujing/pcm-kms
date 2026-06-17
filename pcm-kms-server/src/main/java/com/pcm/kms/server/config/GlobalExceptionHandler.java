package com.pcm.kms.server.config;

import com.pcm.kms.common.exception.KmsException;
import com.pcm.kms.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获业务异常和系统异常，转换为标准 ApiResponse 格式返回。
 * <ul>
 *   <li>业务异常（KmsException）：记录 WARN 级别日志，返回业务错误码和信息</li>
 *   <li>参数校验异常：记录 WARN 级别日志，返回 400 和校验错误信息</li>
 *   <li>系统异常：记录 ERROR 级别日志（含完整堆栈），返回通用提示，不暴露内部细节</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 KMS 业务异常
     * <p>
     * 业务异常通常是参数校验、资源不存在等可预期错误。
     */
    @ExceptionHandler(KmsException.class)
    public ApiResponse<?> handleKmsException(KmsException e) {
        log.warn("KMS业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid 触发）
     * <p>
     * 当 Controller 参数使用 @Valid 校验不通过时触发。
     * 返回所有校验错误信息，方便前端定位问题。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理绑定异常（表单提交等场景）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理未知系统异常
     * <p>
     * 未预期的运行时异常。记录完整堆栈到日志，但只返回通用提示给客户端，
     * 避免泄露堆栈细节、数据库结构等敏感信息。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统内部异常: {}", e.getMessage(), e);
        // 不暴露 e.getMessage()，防止泄露内部实现细节
        return ApiResponse.error(500, "系统内部错误，请联系管理员");
    }
}
