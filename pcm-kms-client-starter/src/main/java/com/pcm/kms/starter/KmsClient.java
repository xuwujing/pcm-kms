package com.pcm.kms.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcm.kms.common.response.ApiResponse;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * KMS 客户端：编程式加解密 API
 * <p>
 * 提供加密、解密、签名、验签、摘要、获取公钥等能力，
 * 自动计算 HMAC-SHA256 签名，支持通过 Jackson ObjectMapper 序列化请求体。
 * <p>
 * 配置示例（application.yml）：
 * <pre>
 * kms:
 *   client:
 *     server-url: http://localhost:8080
 *     client-id: kms_xxx
 *     client-secret: xxx
 *     client-group: default
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "kms.client")
@Data
public class KmsClient {

    /** KMS 服务端地址 */
    private String serverUrl = "http://localhost:8080";
    /** 客户端 ID（启用应用后获得） */
    private String clientId;
    /** 客户端密钥（用于 HMAC 签名） */
    private String clientSecret;
    /** 客户端所属组 */
    private String clientGroup = "default";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 加密
     *
     * @param plainText 明文
     * @param alias     密钥别名
     * @return 加密结果（含 cipherText、algorithm、keyVersion）
     */
    public CryptoResult encrypt(String plainText, String alias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plainText", plainText);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<CryptoResult> resp = post("/api/crypto/encrypt", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 解密（使用最新启用版本的密钥）
     *
     * @param cipherText 密文（Base64）
     * @param alias      密钥别名
     * @return 解密结果（含 plainText）
     */
    public CryptoResult decrypt(String cipherText, String alias) {
        return decrypt(cipherText, alias, null);
    }

    /**
     * 解密（指定密钥版本号，用于解密旧版本加密的历史数据）
     *
     * @param cipherText 密文（Base64）
     * @param alias      密钥别名
     * @param keyVersion 密钥版本号（null 表示使用最新启用版本）
     * @return 解密结果（含 plainText）
     */
    public CryptoResult decrypt(String cipherText, String alias, Integer keyVersion) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cipherText", cipherText);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        if (keyVersion != null) {
            body.put("keyVersion", keyVersion);
        }
        ApiResponse<CryptoResult> resp = post("/api/crypto/decrypt", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 签名
     *
     * @param data  待签名数据
     * @param alias 签名密钥别名
     * @return 签名结果（含 cipherText，即签名值）
     */
    public CryptoResult sign(String data, String alias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<CryptoResult> resp = post("/api/crypto/sign", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 验签
     *
     * @param data      原始数据
     * @param signature 签名值（Base64）
     * @param alias     签名密钥别名
     * @return true=验签通过，false=验签失败
     */
    public boolean verify(String data, String signature, String alias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("signature", signature);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<Boolean> resp = post("/api/crypto/verify", body, Boolean.class);
        return Boolean.TRUE.equals(resp.getData());
    }

    /**
     * 摘要
     *
     * @param plainText 待计算摘要的数据
     * @param algorithm 算法：MD5 或 SM3
     * @return 摘要结果（含 cipherText，即摘要值）
     */
    public CryptoResult digest(String plainText, String algorithm) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plainText", plainText);
        body.put("algorithm", algorithm);
        ApiResponse<CryptoResult> resp = post("/api/crypto/digest", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 获取公钥
     *
     * @param alias 密钥别名
     * @return Base64 编码的公钥
     */
    public String getPublicKey(String alias) {
        String url = serverUrl + "/api/crypto/public-key/" + alias + "?clientGroup=" + clientGroup;
        HttpHeaders headers = buildHeaders("");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class);
        return (String) resp.getBody().getData();
    }

    // ---- 内部方法 ----

    /**
     * 发送 POST 请求
     */
    private <T> ApiResponse<T> post(String path, Map<String, Object> bodyMap, Class<T> type) {
        String url = serverUrl + path;
        String bodyJson = toJson(bodyMap);
        HttpHeaders headers = buildHeaders(bodyJson);
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.POST, entity, ApiResponse.class);
        @SuppressWarnings("unchecked")
        ApiResponse<T> result = (ApiResponse<T>) resp.getBody();
        return result;
    }

    /**
     * 构建请求头，自动计算 HMAC-SHA256 签名
     */
    private HttpHeaders buildHeaders(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (clientId != null && clientSecret != null) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String sign = hmacSha256(clientSecret, body + timestamp + nonce);
            headers.set("X-Client-Id", clientId);
            headers.set("X-Timestamp", timestamp);
            headers.set("X-Nonce", nonce);
            headers.set("X-Sign", sign);
        }
        return headers;
    }

    /**
     * 计算 HMAC-SHA256 签名
     */
    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    /**
     * 使用 Jackson ObjectMapper 序列化为 JSON 字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 加解密结果 DTO
     */
    @Data
    public static class CryptoResult {
        /** 密文（Base64 编码） */
        private String cipherText;
        /** 明文 */
        private String plainText;
        /** 算法 */
        private String algorithm;
        /** 密钥别名 */
        private String alias;
        /** 密钥版本号 */
        private Integer keyVersion;
    }
}
