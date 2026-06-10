# PCM-KMS 快速接入文档

> 5 分钟完成 KMS 接入，为你的应用加上密钥管理能力。

---

## 目录

1. [环境要求](#1-环境要求)
2. [快速启动（SQLite 模式，零依赖）](#2-快速启动sqlite-模式零依赖)
3. [快速启动（MySQL + Redis 模式）](#3-快速启动mysql--redis-模式)
4. [管理后台使用](#4-管理后台使用)
5. [API 调用示例](#5-api-调用示例)
6. [Java SDK 接入](#6-java-sdk-接入)
7. [常见问题](#7-常见问题)

---

## 1. 环境要求

### 后端

| 依赖 | 版本 | 是否必须 |
|------|------|---------|
| JDK | 17+ | ✅ 必须 |
| Maven | 3.6+ | ✅ 必须 |
| MySQL | 8.0+ | ❌ 可选（SQLite 模式不需要） |
| Redis | 7.0+ | ❌ 可选（自动降级 Caffeine） |
| Nacos | 2.x | ❌ 可选（单机模式不需要） |

### 前端

| 依赖 | 版本 | 是否必须 |
|------|------|---------|
| Node.js | 18+ | ✅ 必须 |
| pnpm / npm | — | ✅ 必须 |

---

## 2. 快速启动（SQLite 模式，零依赖）

**适用场景**：本地开发、个人项目、快速体验。

### 2.1 启动后端

```bash
# 克隆项目
git clone https://github.com/your-org/pcm-kms.git
cd pcm-kms

# 编译（跳过测试）
mvn clean package -DskipTests

# 启动（SQLite 模式）
cd pcm-kms-server
java -jar target/pcm-kms-server.jar --spring.profiles.active=sqlite
```

启动成功后访问：
- **Swagger 文档**：http://localhost:8080/doc.html
- **健康检查**：http://localhost:8080/actuator/health

### 2.2 启动前端

```bash
cd pcm-kms-ui

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

访问 http://localhost:5173

**默认管理员账号**：`admin` / `admin123`

---

## 3. 快速启动（MySQL + Redis 模式）

**适用场景**：团队协作、测试环境、生产环境。

### 3.1 Docker Compose 一键启动（推荐）

```bash
cd pcm-kms
docker-compose up -d
```

包含：MySQL 8.0 + Redis 7 + Nacos + KMS Server + KMS UI

### 3.2 手动配置

**1) 初始化数据库**

```bash
mysql -u root -p < sql/mysql/init.sql
```

**2) 修改配置**

`pcm-kms-server/src/main/resources/application-mysql.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pcm_kms?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
    password:
```

**3) 启动**

```bash
java -jar pcm-kms-server.jar --spring.profiles.active=mysql
```

---

## 4. 管理后台使用

### 4.1 创建第一个密钥

1. 登录管理后台 → **密钥管理** → **新增密钥**
2. 填写：
   - **别名**：`my-app-db-pwd`（业务系统中引用此别名）
   - **算法**：选择 `AES`（对称加密）
   - **应用组**：`default`
   - **描述**：数据库密码加密
3. 点击保存

### 4.2 注册客户端应用

1. **客户端管理** → **注册客户端**
2. 填写：
   - **客户端名称**：`my-spring-app`
   - **应用组**：`default`
3. 系统自动生成 `clientId` 和 `clientSecret`，**请妥善保存**

### 4.3 授权密钥给客户端

1. **客户端管理** → 找到刚注册的客户端 → **授权密钥**
2. 勾选 `my-app-db-pwd` → 保存

### 4.4 在线测试加解密

1. **加密测试** 页面
2. 选择密钥别名 `my-app-db-pwd`
3. 输入明文 `MyP@ssw0rd`
4. 点击加密 → 获得密文
5. 点击解密 → 还原明文

---

## 5. API 调用示例

### 5.1 获取 Access Token

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

响应：

```json
{
  "code": 200,
  "data": {
    "token": "satoken-xxxxx"
  }
}
```

### 5.2 客户端签名调用

所有外部 API 调用需要携带签名头：

```bash
# 生成签名（示例）
TIMESTAMP=$(date +%s%3N)
NONCE=$(uuidgen)
BODY='{"plainText":"hello kms","alias":"my-app-db-pwd"}'
SIGN=$(echo -n "${BODY}${TIMESTAMP}${NONCE}" | openssl dgst -sha256 -hmac "your-client-secret" | awk '{print $2}')

curl -X POST http://localhost:8080/api/v1/crypto/encrypt \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: your-client-id" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Nonce: ${NONCE}" \
  -H "X-Sign: ${SIGN}" \
  -d "${BODY}"
```

### 5.3 加密

```bash
curl -X POST http://localhost:8080/api/v1/crypto/encrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: satoken-xxxxx" \
  -d '{
    "plainText": "需要加密的敏感数据",
    "alias": "my-app-db-pwd"
  }'
```

响应：

```json
{
  "code": 200,
  "data": {
    "cipherText": "Base64编码的密文...",
    "algorithm": "aes",
    "alias": "my-app-db-pwd"
  }
}
```

### 5.4 解密

```bash
curl -X POST http://localhost:8080/api/v1/crypto/decrypt \
  -H "Content-Type: application/json" \
  -H "Authorization: satoken-xxxxx" \
  -d '{
    "cipherText": "Base64编码的密文...",
    "alias": "my-app-db-pwd"
  }'
```

### 5.5 获取公钥

```bash
curl http://localhost:8080/api/v1/crypto/public-key/my-rsa-key \
  -H "Authorization: satoken-xxxxx"
```

---

## 6. Java SDK 接入

### 6.1 添加依赖

```xml
<dependency>
    <groupId>com.pcm.kms</groupId>
    <artifactId>pcm-kms-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 6.2 配置文件

```yaml
kms:
  server:
    url: http://localhost:8080          # KMS 服务地址
  client:
    id: your-client-id                  # 客户端 ID
    secret: your-client-secret          # 客户端密钥
  cache:
    ttl: 300                            # 公钥缓存时间（秒）
```

### 6.3 注解式加解密

```java
@RestController
public class UserController {

    // 自动解密请求中的加密字段
    @PostMapping("/user")
    public Result createUser(@KmsDecrypt UserDTO user) {
        // user.phone 和 user.idCard 已自动解密
        userService.save(user);
        return Result.success();
    }

    // 自动加密响应中的敏感字段
    @KmsEncrypt
    @GetMapping("/user/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
}

@Data
public class UserDTO {
    private String name;

    @KmsDecryptField(alias = "user-phone-key")
    private String phone;

    @KmsDecryptField(alias = "user-idcard-key")
    private String idCard;
}
```

### 6.4 编程式调用

```java
@Autowired
private KmsCryptoService kmsCryptoService;

public void demo() {
    // 加密
    CryptoResult result = kmsCryptoService.encrypt("敏感数据", "my-app-db-pwd");
    String cipherText = result.getCipherText();

    // 解密
    String plainText = kmsCryptoService.decrypt(cipherText, "my-app-db-pwd");

    // 获取公钥
    String publicKey = kmsCryptoService.getPublicKey("my-rsa-key");
}
```

### 6.5 数据库字段自动加解密

```java
@Data
@TableName("t_user")
public class User {
    private Long id;
    private String name;

    // 写入时自动加密，读取时自动解密
    @KmsCrypto(alias = "user-phone-key")
    private String phone;
}
```

### 6.6 配置文件加密

`application.yml` 中敏感值可以用 `KMS{...}` 包裹：

```yaml
spring:
  datasource:
    # 原始密码通过 KMS 加密后填入
    password: KMS{encrypted_base64_string}
```

SDK 启动时自动解密。

---

## 7. 常见问题

### Q1: SQLite 模式能用于生产吗？

不建议。SQLite 不支持并发写入，适合开发和单机小项目。生产环境请使用 MySQL。

### Q2: 没有 Redis 怎么办？

系统会自动检测 Redis 是否可用，不可用时降级为 Caffeine 本地缓存，功能不受影响，只是缓存不跨实例共享。

### Q3: 密钥存在数据库安全吗？

私钥和对称密钥使用 `KMS_MASTER_KEY` 环境变量作为主密钥进行 AES-256 加密后存储。请确保主密钥安全保管。

### Q4: 如何切换加密算法？

在管理后台修改密钥元数据的算法字段即可，或调用密钥轮转接口生成新版本密钥。

### Q5: 旧版 KMS 数据能迁移吗？

可以。数据库表结构兼容旧版，直接导出导入即可。注意 clientGroup 和别名需保持一致。

### Q6: 支持国密 SM2/SM4 吗？

支持。创建密钥时选择 SM2（非对称）或 SM4（对称）即可。JDK 17 下需要额外添加 BouncyCastle 安全提供者（SDK 已内置）。

---

## 附录：默认配置速查

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| 后端端口 | 8080 | `server.port` |
| 前端端口 | 5173 | Vite 默认 |
| 管理员账号 | admin / admin123 | 首次启动自动创建 |
| SQLite 文件 | `./data/kms.db` | SQLite 模式 |
| 日志路径 | `./logs/` | 应用日志 |
| Swagger | `/doc.html` | Knife4j 文档 |
