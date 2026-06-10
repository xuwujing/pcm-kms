package com.pcm.kms.starter;

import com.pcm.kms.common.enums.AlgorithmEnum;
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
 */
@Component
@ConfigurationProperties(prefix = "kms.client")
@Data
public class KmsClient {

    private String serverUrl = "http://localhost:8080";
    private String clientId;
    private String clientSecret;
    private String clientGroup = "default";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 加密
     */
    public CryptoResult encrypt(String plainText, String alias) {
        Map<String, String> body = new HashMap<>();
        body.put("plainText", plainText);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<CryptoResult> resp = post("/api/crypto/encrypt", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 解密
     */
    public CryptoResult decrypt(String cipherText, String alias) {
        Map<String, String> body = new HashMap<>();
        body.put("cipherText", cipherText);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<CryptoResult> resp = post("/api/crypto/decrypt", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 签名
     */
    public CryptoResult sign(String data, String alias) {
        Map<String, String> body = new HashMap<>();
        body.put("data", data);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<CryptoResult> resp = post("/api/crypto/sign", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 验签
     */
    public boolean verify(String data, String signature, String alias) {
        Map<String, String> body = new HashMap<>();
        body.put("data", data);
        body.put("signature", signature);
        body.put("alias", alias);
        body.put("clientGroup", clientGroup);
        ApiResponse<Boolean> resp = post("/api/crypto/verify", body, Boolean.class);
        return resp.getData();
    }

    /**
     * 摘要
     */
    public CryptoResult digest(String plainText, String algorithm) {
        Map<String, String> body = new HashMap<>();
        body.put("plainText", plainText);
        body.put("algorithm", algorithm);
        ApiResponse<CryptoResult> resp = post("/api/crypto/digest", body, CryptoResult.class);
        return resp.getData();
    }

    /**
     * 获取公钥
     */
    public String getPublicKey(String alias) {
        String url = serverUrl + "/api/crypto/public-key/" + alias + "?clientGroup=" + clientGroup;
        HttpHeaders headers = buildHeaders("");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class);
        return (String) resp.getBody().getData();
    }

    // ---- internal ----

    private <T> ApiResponse<T> post(String path, Map<String, String> body, Class<T> type) {
        String url = serverUrl + path;
        String bodyJson = toJson(body);
        HttpHeaders headers = buildHeaders(bodyJson);
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);
        ResponseEntity<ApiResponse> resp = restTemplate.exchange(url, HttpMethod.POST, entity, ApiResponse.class);
        return (ApiResponse<T>) resp.getBody();
    }

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

    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            if (e.getValue() == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(e.getValue().replace("\"", "\\\"")).append("\"");
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    @Data
    public static class CryptoResult {
        private String cipherText;
        private String plainText;
        private String algorithm;
        private String alias;
        private Integer keyVersion;
    }
}
