# PCM-KMS 密钥管理系统

> 纯净、轻量、开箱即用的密钥管理系统

## 简介

PCM-KMS 是一个密钥管理系统，用于统一管理应用系统的加密密钥、提供加解密服务、控制密钥访问权限。

**核心特性：**

- 🔐 多算法支持：AES、SM4、RSA、SM2、MD5、SM3、签名验签
- 📦 双数据库模式：MySQL（生产）+ SQLite（开发/单机），自动切换
- 💾 缓存降级：Redis 优先，不可用时自动降级为 Caffeine 本地缓存
- 🔑 多租户隔离：按应用组（clientGroup）隔离密钥，客户端签名鉴权
- 🛡️ 安全防护：密钥加密存储、传输签名、限流控制、审计日志
- 🎨 管理后台：Vue 3 + Element Plus，开箱即用
- 🚀 一键启动：SQLite 模式零依赖，`java -jar` 即可运行
- 🐳 Docker 支持：docker-compose 一键部署

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.x | 应用框架 |
| Spring Cloud | 2021.0.x | 微服务（预留扩展位） |
| MyBatis-Plus | 3.5.x | ORM |
| Sa-Token | 1.37 | 轻量 RBAC 权限认证 |
| BouncyCastle | 1.79 | 国密算法（SM2/SM3/SM4） |
| Knife4j | 4.3 | OpenAPI 3 接口文档 |

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
- Node.js 18+（前端）

### SQLite 模式（零依赖，推荐先体验）

```bash
# 编译
git clone https://github.com/your-org/pcm-kms.git
cd pcm-kms
mvn clean package -DskipTests

# 启动后端
cd pcm-kms-server
java -jar target/pcm-kms-server.jar --spring.profiles.active=sqlite

# 启动前端
cd ../pcm-kms-admin-ui
npm install
npm run dev
```

- 后端：http://localhost:8080
- 前端：http://localhost:5173
- 接口文档：http://localhost:8080/doc.html
- 默认管理员：`admin` / `123456`

### Docker 部署

```bash
docker-compose up -d
```

## 项目结构

```
pcm-kms/
├── pcm-kms-common/          # 公共模块：枚举、异常、工具类
├── pcm-kms-domain/          # 领域模型：实体、DTO
├── pcm-kms-core/            # 核心业务：加密算法引擎
├── pcm-kms-infra/           # 基础设施：MyBatis-Plus、缓存、Flyway
├── pcm-kms-server/          # 主服务：Controller、Service、启动入口
├── pcm-kms-client-starter/  # 客户端 SDK：编程式加解密
├── pcm-kms-admin-ui/        # 前端管理后台
├── sql/                     # 数据库脚本（MySQL + SQLite）
├── docker/                  # Docker 部署文件
├── docs/                    # 设计文档
│   ├── 01-总体设计方案.md
│   ├── 02-开发进度周期说明.md
│   ├── 03-快速接入文档.md
│   ├── 04-框架说明与启动指南.md
│   ├── 05-多智能体协作开发说明.md
│   ├── 06-协作记录模板.md
│   └── 07-开发记录.md
└── pom.xml
```

## 核心 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/apps` | POST | 创建应用 |
| `/api/admin/apps/{id}/enable` | POST | 启用应用（生成凭证和默认密钥） |
| `/api/admin/keys` | POST | 创建密钥 |
| `/api/admin/keys/{id}/rotate` | POST | 密钥轮转 |
| `/api/crypto/encrypt` | POST | 加密 |
| `/api/crypto/decrypt` | POST | 解密 |
| `/api/crypto/sign` | POST | 签名 |
| `/api/crypto/verify` | POST | 验签 |
| `/api/crypto/digest` | POST | 摘要 |
| `/api/crypto/public-key/{alias}` | GET | 获取公钥 |
| `/api/auth/login` | POST | 登录 |

## Java SDK 接入

```xml
<dependency>
    <groupId>com.pcm.kms</groupId>
    <artifactId>pcm-kms-client-starter</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

```yaml
kms:
  client:
    server-url: http://localhost:8080
    client-id: your-client-id
    client-secret: your-client-secret
    client-group: default
```

```java
@Autowired
private KmsClient kmsClient;

// 加密
CryptoResult encrypted = kmsClient.encrypt("敏感数据", "my-alias");

// 解密
CryptoResult decrypted = kmsClient.decrypt(encrypted.getCipherText(), "my-alias");
```

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `kms.security.strict-sign` | false | 是否强制客户端签名验证 |
| `kms.security.request-expire-seconds` | 300 | 请求有效期（秒） |
| `kms.ratelimit.enabled` | true | 是否启用限流 |
| `kms.ratelimit.max-per-minute` | 60 | 每分钟最大请求数 |
| `spring.profiles.active` | sqlite | 数据库模式：sqlite / mysql |

## 版本

| 版本 | 说明 |
|------|------|
| v0.1.0 | 项目骨架 + 基础框架 |
| v0.2.0 | 密钥管理 + 加解密 + 前端 + SDK + 限流 + Docker |

## 开源协议

MIT License
