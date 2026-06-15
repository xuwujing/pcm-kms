package com.pcm.kms.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KmsServerSmokeTest {

    private static Path sqliteDir;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        sqliteDir = Files.createTempDirectory("pcm-kms-test-");
        registry.add("spring.profiles.active", () -> "sqlite");
        registry.add("kms.sqlite.data-path", () -> sqliteDir.toAbsolutePath().toString());
        registry.add("kms.security.strict-sign", () -> false);
        registry.add("kms.ratelimit.enabled", () -> false);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteCoreFlow() throws Exception {
        JsonNode unauthorizedApps = get("/api/admin/apps?page=1&size=20", null);
        assertEquals(401, unauthorizedApps.get("code").asInt());

        String token = loginAndGetToken();
        HttpHeaders headers = authHeaders(token);

        String clientId = "kms_test_app";
        JsonNode createdApp = post("/api/admin/apps", headers, mapOf(
                "clientId", clientId,
                "clientName", "KMS Test App",
                "clientGroup", "default",
                "contacts", "tester"
        ));
        assertEquals(0, createdApp.get("code").asInt());
        long appId = createdApp.path("data").path("id").asLong();
        assertTrue(appId > 0);

        JsonNode enabledApp = postWithoutBody("/api/admin/apps/" + appId + "/enable", headers);
        assertEquals(0, enabledApp.get("code").asInt());
        assertTrue(enabledApp.path("data").path("enabled").asBoolean());
        assertFalse(enabledApp.path("data").path("clientSecret").asText().isEmpty());

        JsonNode createdKey = post("/api/admin/keys", headers, mapOf(
                "clientId", clientId,
                "alias", "phone-aes",
                "algorithm", "aes",
                "description", "phone encrypt key"
        ));
        assertEquals(0, createdKey.get("code").asInt());

        JsonNode encrypted = post("/api/crypto/encrypt", null, mapOf(
                "alias", "phone-aes",
                "clientGroup", "default",
                "plainText", "13800138000"
        ));
        assertEquals(0, encrypted.get("code").asInt());
        String cipherText = encrypted.path("data").path("cipherText").asText();
        assertFalse(cipherText.isEmpty());

        JsonNode decrypted = post("/api/crypto/decrypt", null, mapOf(
                "alias", "phone-aes",
                "clientGroup", "default",
                "cipherText", cipherText
        ));
        assertEquals(0, decrypted.get("code").asInt());
        assertEquals("13800138000", decrypted.path("data").path("plainText").asText());

        JsonNode digested = post("/api/crypto/digest", null, mapOf(
                "algorithm", "md5",
                "plainText", "hello"
        ));
        assertEquals(0, digested.get("code").asInt());
        assertEquals("XUFAKrxLKna5cZ2REBfFkg==", digested.path("data").path("cipherText").asText());
    }

    private String loginAndGetToken() throws Exception {
        JsonNode login = post("/api/auth/login", null, mapOf(
                "username", "admin",
                "password", "123456"
        ));
        assertEquals(0, login.get("code").asInt());
        String token = login.path("data").path("token").asText();
        assertFalse(token.isEmpty());
        return token;
    }

    private JsonNode get(String path, HttpHeaders headers) throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(headers == null ? new HttpHeaders() : headers);
        ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.GET, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode postWithoutBody(String path, HttpHeaders headers) throws Exception {
        HttpEntity<String> entity = new HttpEntity<>(headers == null ? new HttpHeaders() : headers);
        ResponseEntity<String> response = restTemplate.exchange(url(path), HttpMethod.POST, entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private JsonNode post(String path, HttpHeaders headers, Map<String, Object> body) throws Exception {
        HttpHeaders actualHeaders = headers == null ? new HttpHeaders() : new HttpHeaders(headers);
        actualHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), actualHeaders);
        ResponseEntity<String> response = restTemplate.postForEntity(url(path), entity, String.class);
        return objectMapper.readTree(response.getBody());
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("satoken", token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}
