# PCM-KMS 密钥管理系统

> 纯净、轻量、开箱即用的密钥管理系统

## 简介

PCM-KMS 是一个密钥管理系统，用于统一管理应用系统的加密密钥、提供加解密服务、控制密钥访问权限。

**核心特性：**

- 🔐 多算法支持：AES、SM4、RSA、SM2、MD5、SM3、签名验签
- 📦 双数据库模式：MySQL（生产）+ SQLite（开发/单机），自动切换
- 💾 缓存降级：Redis 优先，不可用时自动降级为 Caffeine 本地缓存
- 🔑 多租户隔离：按应用组（clientGroup）隔离密钥，客户端签名鉴权
- 🛡️ 安全防护：主密钥加密存储、BCrypt 密码、传输签名、nonce 防重放、限流控制、审计日志
- 🎨 管理后台：Vue 3 + Element Plus，开箱即用
- 🚀 一键启动：SQLite 模式零依赖，`java -jar` 即可运行
- 🐳 Docker 支持：docker-compose 一键部署

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.x | 应用框架 |
| MyBatis-Plus | 3.5.x | ORM |
| Sa-Token | 1.37 | 轻量 RBAC 权限认证 |
| BouncyCastle | 1.79 | 国密算法（SM2/SM3/SM4） |
| Knife4j | 4.3 | OpenAPI 3 接口文档 |
| Caffeine | 2.9.3 | 本地缓存（nonce/限流计数器） |
| Spring Security Crypto | 5.8.6 | BCrypt 密码加密 |
| Flyway | 9.x | 数据库版本管理 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.x | 前端框架 |
| Element Plus | 2.x | UI 组件库 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- Node.js 18+（前端，可选）

### 方式一：SQLite 模式（零依赖，推荐首次体验）

```bash
# 1. 克隆项目
git clone https://github.com/your-org/pcm-kms.git
cd pcm-kms

# 2. 编译
mvn clean package -DskipTests

# 3. 启动后端（SQLite 模式，无需 MySQL/Redis）
cd pcm-kms-server
java -jar target/pcm-kms-server.jar --spring.profiles.active=sqlite

# 4.（可选）启动前端管理后台
cd ../pcm-kms-admin-ui
npm install
npm run dev
```

启动后：
- 后端 API：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html
- 前端管理后台：http://localhost:5173
- 默认管理员：`admin` / `123456`

### 方式二：MySQL + Redis 模式（生产部署）

```bash
# 1. 准备 MySQL 和 Redis

# 2. 修改配置
# 编辑 pcm-kms-server/src/main/resources/application.yml
# 填写 MySQL 连接信息和 Redis 地址

# 3. 编译启动
mvn clean package -DskipTests
cd pcm-kms-server
java -jar target/pcm-kms-server.jar
```

### 方式三：Docker 一键部署

```bash
docker-compose up -d
```

## 使用指南

### 第一步：创建应用

1. 登录管理后台 http://localhost:5173
2. 进入「应用管理」，点击「创建应用」
3. 填写应用名称、联系人等信息
4. 创建后点击「启用」，系统自动生成：
   - `client_id` — 应用标识
   - `client_secret` — 应用密钥（用于签名验证）
   - 默认 AES 密钥（别名自动生成）

### 第二步：创建业务密钥

1. 进入「密钥管理」，点击「创建密钥」
2. 选择算法类型：
   - **对称加密**：AES（通用）、SM4（国密）
   - **非对称加密**：RSA（通用）、SM2（国密）
   - **签名**：SHA256withRSA
   - **摘要**：MD5、SM3（无需密钥）
3. 设置别名（alias），如 `user-phone-aes`、`order-sign-rsa`
4. 启用密钥

### 第三步：业务系统接入

#### 方式 A：Java SDK 接入（推荐）

1. 添加 Maven 依赖：

```xml
<dependency>
    <groupId>com.pcm.kms</groupId>
    <artifactId>pcm-kms-client-starter</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

2. 配置 application.yml：

```yaml
kms:
  client:
    server-url: http://localhost:8080    # KMS 服务端地址
    client-id: kms_xxxxxxxx              # 在管理后台启用应用后获得
    client-secret: your-client-secret    # 在管理后台启用应用后获得
    client-group: default                # 应用组（多租户隔离）
```

3. 编程式调用：

```java
@Autowired
private KmsClient kmsClient;

// 加密
KmsClient.CryptoResult encrypted = kmsClient.encrypt("敏感数据", "user-phone-aes");
String cipherText = encrypted.getCipherText();

// 解密
KmsClient.CryptoResult decrypted = kmsClient.decrypt(cipherText, "user-phone-aes");
String plainText = decrypted.getPlainText();

// 签名
KmsClient.CryptoResult signed = kmsClient.sign("待签名数据", "order-sign-rsa");

// 验签
boolean valid = kmsClient.verify("待签名数据", signed.getCipherText(), "order-sign-rsa");

// 摘要（无需密钥别名，直接指定算法）
KmsClient.CryptoResult digest = kmsClient.digest("数据", "SM3");
```

#### 方式 B：HTTP API 直接调用

```http
POST /api/crypto/encrypt
Content-Type: application/json
X-Client-Id: kms_xxxxxxxx
X-Timestamp: 1718000000000
X-Nonce: 6f3c6ad2e5b14b95
X-Sign: xxxxxxxxxxxxx

{
  "alias": "user-phone-aes",
  "plainText": "13800138000",
  "clientGroup": "default"
}
```

> 签名计算方式：`HMAC-SHA256(clientSecret, body + timestamp + nonce)`
> 
> 开发模式可关闭签名验证：`kms.security.strict-sign=false`

### 运行示例项目

项目内置了 demo 示例，展示完整的接入流程：

```bash
# 1. 先启动 KMS 服务端
cd pcm-kms-server
java -jar target/pcm-kms-server.jar --spring.profiles.active=sqlite

# 2. 在管理后台创建应用、启用应用、创建密钥

# 3. 修改 demo 配置（填写你的 clientId/clientSecret）
# 编辑 pcm-kms-demo/src/main/resources/application.yml

# 4. 启动 demo
cd pcm-kms-demo
mvn spring-boot:run

# 5. 测试
curl -X POST "http://localhost:8081/demo/encrypt-decrypt?alias=user-phone-aes&plainText=HelloKMS"
```

Demo 提供的接口：
| 接口 | 说明 |
|------|------|
| `POST /demo/encrypt` | 加密示例 |
| `POST /demo/decrypt` | 解密示例 |
| `POST /demo/encrypt-decrypt` | 加解密闭环验证 |
| `POST /demo/sign` | 签名示例 |
| `POST /demo/verify` | 验签示例 |
| `POST /demo/sign-verify` | 签名验签闭环验证 |
| `POST /demo/digest` | 摘要示例 |
| `GET /demo/public-key` | 获取公钥示例 |
| `POST /demo/scenario/phone` | 手机号加密存储场景模拟 |

## 项目结构

```
pcm-kms/
├── pcm-kms-common/          # 公共模块：枚举、异常、工具类
├── pcm-kms-domain/          # 领域模型：实体、DTO
├── pcm-kms-core/            # 核心业务：加密算法引擎（策略模式）
├── pcm-kms-infra/           # 基础设施：MyBatis-Plus、缓存、Flyway
├── pcm-kms-server/          # 主服务：Controller、Service、启动入口
├── pcm-kms-client-starter/  # 客户端 SDK：编程式加解密
├── pcm-kms-admin-ui/        # 前端管理后台
├── pcm-kms-demo/            # 示例项目：演示如何接入
├── pcm-kms-stress-test/     # 压力测试：百万级性能验证
├── sql/                     # 数据库脚本（MySQL + SQLite）
├── docker/                  # Docker 部署文件
└── docs/                    # 设计文档
```

## 核心 API

### 管理接口（需登录）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 登录 |
| `/api/admin/apps` | POST | 创建应用 |
| `/api/admin/apps` | GET | 应用列表 |
| `/api/admin/apps/{id}` | GET | 应用详情 |
| `/api/admin/apps/{id}/enable` | POST | 启用应用（生成凭证和默认密钥） |
| `/api/admin/keys` | POST | 创建密钥 |
| `/api/admin/keys` | GET | 密钥列表 |
| `/api/admin/keys/{id}` | GET | 密钥详情 |
| `/api/admin/keys/{id}/enable` | POST | 启用密钥 |
| `/api/admin/keys/{id}/disable` | POST | 禁用密钥 |
| `/api/admin/keys/{id}/rotate` | POST | 密钥轮转 |

### 加解密接口（需签名）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/crypto/encrypt` | POST | 加密 |
| `/api/crypto/decrypt` | POST | 解密 |
| `/api/crypto/sign` | POST | 签名 |
| `/api/crypto/verify` | POST | 验签 |
| `/api/crypto/digest` | POST | 摘要 |
| `/api/crypto/public-key/{alias}` | GET | 获取公钥 |

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.profiles.active` | sqlite | 数据库模式：sqlite / mysql |
| `kms.security.strict-sign` | false | 是否强制客户端签名验证（生产建议 true） |
| `kms.security.request-expire-seconds` | 300 | 请求有效期（秒） |
| `kms.ratelimit.enabled` | true | 是否启用限流 |
| `kms.ratelimit.max-per-minute` | 60 | 每分钟最大请求数 |
| `kms.cors.allowed-origins` | * | 允许的跨域域名（逗号分隔，生产环境请指定具体域名） |
| `PCM_KMS_MASTER_KEY` | — | 主密钥（环境变量，用于密钥材料加密存储） |

## 安全说明

- **主密钥加密存储**：私钥和对称密钥使用主密钥（AES-256-CBC）二次加密后落库，主密钥通过环境变量 `PCM_KMS_MASTER_KEY` 注入
- **BCrypt 密码**：管理后台密码使用 BCrypt 哈希存储（兼容旧版 MD5 自动迁移）
- **SM4 CBC 模式**：SM4 使用 CBC + 随机 IV 加密，相同明文产生不同密文
- **传输签名**：所有加解密 API 请求需携带 HMAC-SHA256 签名，防止篡改和重放
- **nonce 防重放**：基于 Caffeine 缓存的 nonce 去重（50000 条 + 10 分钟 TTL 自动过期）
- **限流控制**：基于 IP/clientId 的分钟级滑动窗口限流（Caffeine 缓存 + 1 分钟自动清理）
- **异常信息保护**：系统异常不暴露内部细节，只返回通用提示
- **审计日志**：所有加解密操作记录审计日志，不记录明文

## 压力测试

内置百万级压力测试工具：

```bash
# 编译
mvn clean package -DskipTests

# 运行压力测试（100万次，10并发）
cd pcm-kms-stress-test
java -jar target/pcm-kms-stress-test.jar \
  --stress.enabled=true \
  --stress.alias=stress-aes \
  --stress.count=1000000 \
  --stress.threads=10
```

测试内容：
1. 百万次加密性能测试
2. 百万次加解密闭环测试
3. 百万次摘要性能测试（SM3）

输出指标：总耗时、平均耗时（μs）、TPS、成功率。

## 常见问题

### 没有 MySQL 能启动吗？

可以。使用 SQLite 模式：`--spring.profiles.active=sqlite`，零依赖启动。

### 没有 Redis 能启动吗？

可以。系统自动降级为 Caffeine 本地缓存，无需额外配置。

### 如何生成主密钥？

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

然后通过环境变量注入：`PCM_KMS_MASTER_KEY=你生成的密钥`

### 业务为什么用 alias 而不是 secret_id？

- alias 语义更稳定，适合版本管理
- alias 适合权限控制
- 业务无需感知内部 secret_id

### 如何关闭签名验证（开发调试）？

```yaml
kms:
  security:
    strict-sign: false
```

### Docker 部署如何持久化数据？

SQLite 模式挂载数据目录：

```yaml
volumes:
  - ./data:/app/data
```

MySQL 模式参考 docker-compose.yml 中的 MySQL 配置。

## 版本

| 版本 | 说明 |
|------|------|
| v0.1.0 | 项目骨架 + 基础框架 |
| v0.2.0 | 密钥管理 + 加解密 + 前端 + SDK + 限流 + Docker |
| v0.3.0 | 安全增强：主密钥加密存储、BCrypt 密码、SM4 CBC、Caffeine 缓存、密钥轮转优化、Bug 修复 |

## 开源协议

MIT License
