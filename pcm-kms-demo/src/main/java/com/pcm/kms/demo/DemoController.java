package com.pcm.kms.demo;

import com.pcm.kms.starter.KmsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Demo 控制器：展示 KmsClient 的完整使用方式
 * <p>
 * 包含：加密、解密、签名、验签、摘要、获取公钥等全部 API 调用示例。
 * 每个接口都有详细注释，方便理解用法。
 */
@Slf4j
@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    private KmsClient kmsClient;

    // ==================== 对称加密示例（AES/SM4） ====================

    /**
     * 加密示例
     * <p>
     * 使用方式：POST /demo/encrypt?alias=my-aes-key&plainText=13800138000
     * <p>
     * alias 是在 KMS 管理后台创建密钥时设置的别名，如 "user-phone-aes"
     * plainText 是要加密的明文
     */
    @PostMapping("/encrypt")
    public Map<String, Object> encrypt(@RequestParam String alias,
                                       @RequestParam String plainText) {
        log.info(">>> 加密请求: alias={}, plainText={}***", alias,
                plainText.length() > 3 ? plainText.substring(0, 3) : "***");

        KmsClient.CryptoResult result = kmsClient.encrypt(plainText, alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("操作", "加密");
        resp.put("别名", alias);
        resp.put("明文", plainText);
        resp.put("密文", result.getCipherText());
        resp.put("算法", result.getAlgorithm());
        resp.put("密钥版本", result.getKeyVersion());
        return resp;
    }

    /**
     * 解密示例
     * <p>
     * 使用方式：POST /demo/decrypt?alias=my-aes-key&cipherText=Base64CipherText
     */
    @PostMapping("/decrypt")
    public Map<String, Object> decrypt(@RequestParam String alias,
                                       @RequestParam String cipherText) {
        log.info(">>> 解密请求: alias={}", alias);

        KmsClient.CryptoResult result = kmsClient.decrypt(cipherText, alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("操作", "解密");
        resp.put("别名", alias);
        resp.put("密文", cipherText);
        resp.put("明文", result.getPlainText());
        resp.put("算法", result.getAlgorithm());
        resp.put("密钥版本", result.getKeyVersion());
        return resp;
    }

    /**
     * 加解密完整流程示例
     * <p>
     * 一步完成：先加密再解密，验证闭环。适合快速验证接入是否成功。
     * 使用方式：POST /demo/encrypt-decrypt?alias=my-aes-key&plainText=HelloKMS
     */
    @PostMapping("/encrypt-decrypt")
    public Map<String, Object> encryptAndDecrypt(@RequestParam String alias,
                                                  @RequestParam String plainText) {
        log.info(">>> 加解密闭环测试: alias={}, plainText={}", alias, plainText);

        // 第一步：加密
        KmsClient.CryptoResult encrypted = kmsClient.encrypt(plainText, alias);
        log.info("加密结果: cipherText={}", encrypted.getCipherText());

        // 第二步：解密
        KmsClient.CryptoResult decrypted = kmsClient.decrypt(encrypted.getCipherText(), alias);
        log.info("解密结果: plainText={}", decrypted.getPlainText());

        // 第三步：验证
        boolean success = plainText.equals(decrypted.getPlainText());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("原始明文", plainText);
        resp.put("加密后密文", encrypted.getCipherText());
        resp.put("解密后明文", decrypted.getPlainText());
        resp.put("验证结果", success ? "✅ 加解密一致" : "❌ 加解密不一致");
        resp.put("算法", encrypted.getAlgorithm());
        resp.put("密钥版本", encrypted.getKeyVersion());
        return resp;
    }

    // ==================== 非对称加密示例（RSA/SM2） ====================

    /**
     * 签名示例
     * <p>
     * 使用方式：POST /demo/sign?alias=my-sign-key&data=important-message
     * <p>
     * 注意：签名需要 RSA 或 SM2 类型的密钥
     */
    @PostMapping("/sign")
    public Map<String, Object> sign(@RequestParam String alias,
                                    @RequestParam String data) {
        log.info(">>> 签名请求: alias={}, data={}", alias, data);

        KmsClient.CryptoResult result = kmsClient.sign(data, alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("操作", "签名");
        resp.put("别名", alias);
        resp.put("原始数据", data);
        resp.put("签名值", result.getCipherText());
        resp.put("算法", result.getAlgorithm());
        return resp;
    }

    /**
     * 验签示例
     * <p>
     * 使用方式：POST /demo/verify?alias=my-sign-key&data=important-message&signature=Base64Signature
     */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestParam String alias,
                                      @RequestParam String data,
                                      @RequestParam String signature) {
        log.info(">>> 验签请求: alias={}, data={}", alias, data);

        boolean valid = kmsClient.verify(data, signature, alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("操作", "验签");
        resp.put("别名", alias);
        resp.put("原始数据", data);
        resp.put("签名值", signature);
        resp.put("验签结果", valid ? "✅ 签名有效" : "❌ 签名无效");
        return resp;
    }

    /**
     * 签名+验签完整流程示例
     * <p>
     * 一步完成：先签名再验签，验证闭环。
     * 使用方式：POST /demo/sign-verify?alias=my-sign-key&data=important-message
     */
    @PostMapping("/sign-verify")
    public Map<String, Object> signAndVerify(@RequestParam String alias,
                                             @RequestParam String data) {
        log.info(">>> 签名验签闭环测试: alias={}, data={}", alias, data);

        // 第一步：签名
        KmsClient.CryptoResult signed = kmsClient.sign(data, alias);
        log.info("签名结果: signature={}", signed.getCipherText());

        // 第二步：验签
        boolean valid = kmsClient.verify(data, signed.getCipherText(), alias);
        log.info("验签结果: valid={}", valid);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("原始数据", data);
        resp.put("签名值", signed.getCipherText());
        resp.put("验签结果", valid ? "✅ 签名验签通过" : "❌ 签名验签失败");
        resp.put("算法", signed.getAlgorithm());
        return resp;
    }

    // ==================== 摘要示例（MD5/SM3） ====================

    /**
     * 摘要示例
     * <p>
     * 使用方式：POST /demo/digest?plainText=HelloWorld&algorithm=SM3
     * <p>
     * algorithm 可选：MD5、SM3
     */
    @PostMapping("/digest")
    public Map<String, Object> digest(@RequestParam String plainText,
                                      @RequestParam(defaultValue = "SM3") String algorithm) {
        log.info(">>> 摘要请求: algorithm={}, plainText={}", algorithm, plainText);

        KmsClient.CryptoResult result = kmsClient.digest(plainText, algorithm);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("操作", "摘要");
        resp.put("明文", plainText);
        resp.put("摘要值", result.getCipherText());
        resp.put("算法", result.getAlgorithm());
        return resp;
    }

    // ==================== 公钥查询示例 ====================

    /**
     * 获取公钥示例
     * <p>
     * 使用方式：GET /demo/public-key?alias=my-rsa-key
     * <p>
     * 适用于：业务端需要用公钥自行加密数据（如前端加密传到后端）
     */
    @GetMapping("/public-key")
    public Map<String, Object> getPublicKey(@RequestParam String alias) {
        log.info(">>> 获取公钥: alias={}", alias);

        String publicKey = kmsClient.getPublicKey(alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("别名", alias);
        resp.put("公钥", publicKey);
        return resp;
    }

    // ==================== 业务场景模拟 ====================

    /**
     * 模拟业务场景：用户手机号加密存储
     * <p>
     * 展示真实业务中如何使用 KMS：
     * 1. 用户注册时加密手机号存入数据库
     * 2. 查询用户时解密手机号展示
     * <p>
     * 使用方式：POST /demo/scenario/phone?alias=user-phone-aes&phone=13800138000
     */
    @PostMapping("/scenario/phone")
    public Map<String, Object> phoneEncryptionScenario(@RequestParam String alias,
                                                       @RequestParam String phone) {
        log.info(">>> 业务场景模拟: 手机号加密存储, phone={}***", phone.substring(0, 3));

        // 模拟注册：加密手机号后存库
        KmsClient.CryptoResult encrypted = kmsClient.encrypt(phone, alias);
        String storedInDb = encrypted.getCipherText(); // 这是存到数据库的值

        // 模拟查询：从数据库取出密文，解密展示
        KmsClient.CryptoResult decrypted = kmsClient.decrypt(storedInDb, alias);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("场景", "用户手机号加密存储");
        resp.put("用户输入的手机号", phone);
        resp.put("存入数据库的密文", storedInDb);
        resp.put("查询时解密的手机号", decrypted.getPlainText());
        resp.put("数据一致性", phone.equals(decrypted.getPlainText()) ? "✅ 一致" : "❌ 不一致");
        return resp;
    }
}
