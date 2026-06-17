package com.pcm.kms.server.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pcm.kms.domain.model.ClientApp;
import com.pcm.kms.server.service.ClientAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 客户端签名验证拦截器
 * <p>
 * 对 /api/crypto/** 路径下的接口进行 HMAC-SHA256 签名校验。
 * <p>
 * 请求需携带以下 Header：
 * <ul>
 *   <li>X-Client-Id: 客户端ID</li>
 *   <li>X-Timestamp: 毫秒时间戳</li>
 *   <li>X-Nonce: 随机字符串（防重放）</li>
 *   <li>X-Sign: HMAC-SHA256(clientSecret, body + timestamp + nonce)</li>
 * </ul>
 * <p>
 * 验证步骤：
 * <ol>
 *   <li>检查时间戳是否在有效期内（默认 5 分钟）</li>
 *   <li>检查 nonce 是否已使用（Caffeine 缓存 + TTL 自动过期）</li>
 *   <li>根据 clientId 查询 clientSecret</li>
 *   <li>读取请求 body，重新计算签名并比对</li>
 * </ol>
 * <p>
 * 配置项：
 * <ul>
 *   <li>kms.security.strict-sign: 是否强制验签（false 时跳过验签，方便开发调试）</li>
 *   <li>kms.security.request-expire-seconds: 请求有效期（秒，默认 300）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureVerifyInterceptor implements HandlerInterceptor {

    private final ClientAppService clientAppService;

    /** 是否强制验签（开发模式可关闭） */
    private boolean strictSign = false;

    /** 请求有效期（秒） */
    private int requestExpireSeconds = 300;

    /**
     * nonce 缓存（防重放）
     * <p>
     * 使用 Caffeine 缓存，key 为 nonce 字符串，value 为 Boolean.TRUE。
     * 过期时间设为请求有效期的 2 倍，确保过期的请求不会被重放。
     * 最大缓存 50000 条，超出后按 LRU 淘汰。
     */
    private final Cache<String, Boolean> nonceCache = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 非严格模式直接放行
        if (!strictSign) {
            return true;
        }

        String clientId = request.getHeader("X-Client-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");

        // 检查必要 Header
        if (clientId == null || timestamp == null || nonce == null || sign == null) {
            log.warn("签名验证失败: 缺少必要Header, clientId={}", clientId);
            writeSignFailResponse(response, "缺少签名相关Header");
            return false;
        }

        // 1. 检查时间戳有效性
        try {
            long reqTime = Long.parseLong(timestamp);
            long diff = Math.abs(System.currentTimeMillis() - reqTime);
            if (diff > requestExpireSeconds * 1000L) {
                log.warn("签名验证失败: 请求已过期, clientId={}, diff={}ms", clientId, diff);
                writeSignFailResponse(response, "请求已过期");
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("签名验证失败: 时间戳格式错误, clientId={}, timestamp={}", clientId, timestamp);
            writeSignFailResponse(response, "时间戳格式错误");
            return false;
        }

        // 2. 检查 nonce 防重放（Caffeine 缓存自动 TTL 过期）
        if (nonceCache.getIfPresent(nonce) != null) {
            log.warn("签名验证失败: nonce重复, clientId={}, nonce={}", clientId, nonce);
            writeSignFailResponse(response, "请求已过期（nonce重复）");
            return false;
        }
        nonceCache.put(nonce, Boolean.TRUE);

        // 3. 查询 clientSecret
        ClientApp app = clientAppService.getByClientId(clientId);
        if (app == null || !app.getEnabled()) {
            log.warn("签名验证失败: 客户端不存在或未启用, clientId={}", clientId);
            writeSignFailResponse(response, "客户端不存在或未启用");
            return false;
        }

        // 4. 读取请求 body 并计算签名
        // 依赖 ContentCachingFilter 将 body 缓存到 ContentCachingRequestWrapper 中
        String body = "";
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapped = (ContentCachingRequestWrapper) request;
            byte[] buf = wrapped.getContentAsByteArray();
            if (buf.length > 0) {
                body = new String(buf, StandardCharsets.UTF_8);
            }
        }

        String data = body + timestamp + nonce;
        String expectedSign = hmacSha256(app.getClientSecret(), data);
        if (!expectedSign.equals(sign)) {
            log.warn("签名验证失败: 签名不匹配, clientId={}", clientId);
            writeSignFailResponse(response, "签名验证失败");
            return false;
        }

        log.debug("签名验证通过: clientId={}", clientId);
        return true;
    }

    /**
     * HMAC-SHA256 签名计算
     *
     * @param key  签名密钥（clientSecret）
     * @param data 待签名数据（body + timestamp + nonce）
     * @return Base64 编码的签名值
     */
    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("签名计算异常", e);
            return "";
        }
    }

    /**
     * 写入验签失败响应
     */
    private void writeSignFailResponse(HttpServletResponse response, String message) throws java.io.IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"timestamp\":" + System.currentTimeMillis() + "}");
    }

    public void setStrictSign(boolean strictSign) {
        this.strictSign = strictSign;
    }

    public void setRequestExpireSeconds(int requestExpireSeconds) {
        this.requestExpireSeconds = requestExpireSeconds;
    }
}
