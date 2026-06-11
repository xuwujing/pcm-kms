# PCM-KMS Demo 示例项目

> 一个最小化的 Spring Boot 示例应用，演示如何接入 PCM-KMS 使用加解密、签名验签等功能。

## 前置条件

1. **PCM-KMS 服务端已启动**（默认 `http://localhost:8080`）
2. 已在管理后台完成以下操作：
   - 创建应用
   - 启用应用（获取 `clientId` 和 `clientSecret`）
   - 创建密钥（记住 `alias`，如 `user-phone-aes`）

## 快速开始

### 1. 修改配置

编辑 `src/main/resources/application.yml`，填入你的接入凭证：

```yaml
kms:
  client:
    server-url: http://localhost:8080
    client-id: kms_xxxxxxxx      # 替换为你的 clientId
    client-secret: your-secret   # 替换为你的 clientSecret
    client-group: default
```

### 2. 编译

```bash
# 在项目根目录
mvn clean package -DskipTests -pl pcm-kms-demo -am
```

### 3. 启动

```bash
cd pcm-kms-demo
java -jar target/pcm-kms-demo.jar
```

或直接用 Maven 运行：

```bash
mvn spring-boot:run -pl pcm-kms-demo
```

### 4. 测试

Demo 启动后监听 `8081` 端口，以下是所有可用接口：

#### 对称加密（AES/SM4）

```bash
# 加密
curl -X POST "http://localhost:8081/demo/encrypt?alias=user-phone-aes&plainText=13800138000"

# 解密
curl -X POST "http://localhost:8081/demo/decrypt?alias=user-phone-aes&cipherText=Base64密文"

# 加解密闭环测试（一步验证）
curl -X POST "http://localhost:8081/demo/encrypt-decrypt?alias=user-phone-aes&plainText=13800138000"
```

#### 签名验签（RSA/SM2）

```bash
# 签名
curl -X POST "http://localhost:8081/demo/sign?alias=api-sign-rsa&data=important-message"

# 验签
curl -X POST "http://localhost:8081/demo/verify?alias=api-sign-rsa&data=important-message&signature=Base64签名值"

# 签名验签闭环测试
curl -X POST "http://localhost:8081/demo/sign-verify?alias=api-sign-rsa&data=important-message"
```

#### 摘要（MD5/SM3）

```bash
# SM3 摘要
curl -X POST "http://localhost:8081/demo/digest?plainText=HelloWorld&algorithm=SM3"

# MD5 摘要
curl -X POST "http://localhost:8081/demo/digest?plainText=HelloWorld&algorithm=MD5"
```

#### 获取公钥

```bash
curl "http://localhost:8081/demo/public-key?alias=my-rsa-key"
```

#### 业务场景模拟

```bash
# 模拟用户手机号加密存储场景
curl -X POST "http://localhost:8081/demo/scenario/phone?alias=user-phone-aes&phone=13800138000"
```

## 代码结构

```
pcm-kms-demo/
├── pom.xml                              # Maven 配置，依赖 pcm-kms-client-starter
├── README.md                            # 本文件
└── src/main/
    ├── java/com/pcm/kms/demo/
    │   ├── DemoApplication.java         # 启动类
    │   ├── DemoController.java          # REST 接口示例（全部 API 调用方式）
    │   └── DemoService.java             # Service 层使用示例
    └── resources/
        └── application.yml              # 配置文件（含详细注释）
```

## 两种接入方式

### 方式一：编程式调用（推荐）

在业务代码中直接注入 `KmsClient`：

```java
@Autowired
private KmsClient kmsClient;

// 加密
KmsClient.CryptoResult encrypted = kmsClient.encrypt("敏感数据", "my-alias");
String cipherText = encrypted.getCipherText();

// 解密
KmsClient.CryptoResult decrypted = kmsClient.decrypt(cipherText, "my-alias");
String plainText = decrypted.getPlainText();
```

### 方式二：REST API 直接调用

不使用 SDK，直接调用 KMS 服务端接口：

```bash
# 加密
curl -X POST http://localhost:8080/api/crypto/encrypt \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: your-client-id" \
  -H "X-Timestamp: 1718000000000" \
  -H "X-Nonce: random-string" \
  -H "X-Sign: hmac-sha256-signature" \
  -d '{"alias":"user-phone-aes","plainText":"13800138000","clientGroup":"default"}'
```

> 推荐使用 SDK（方式一），SDK 会自动处理签名、时间戳、nonce 等安全头。

## 常见问题

### 启动报连接拒绝？

确认 KMS 服务端已启动，且 `server-url` 配置正确。

### 报签名验证失败？

- 检查 `client-id` 和 `client-secret` 是否正确
- 如果是开发环境，可以在服务端关闭严格验签：`kms.security.strict-sign=false`

### 报密钥别名不存在？

先在管理后台创建密钥，并确保 alias 与代码中一致。
