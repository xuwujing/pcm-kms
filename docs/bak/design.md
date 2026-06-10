# PCM-KMS 密钥管理系统 — 设计方案

> 版本：v1.0 | 作者：pcm | 日期：2026-06-10

---

## 1. 项目背景

### 1.1 为什么重写

上一版 KMS（`D:\pcm\project\kms`）已完成核心功能，但存在以下问题：

| 问题 | 旧版现状 | 新版目标 |
|------|---------|---------|
| 技术栈陈旧 | Java 8 + Dubbo + Zookeeper + war 部署 | Java 17 + Spring Cloud + Nacos + jar 部署 |
| 过度依赖公司内部框架 | bajcommon、autopilot、bnt-client 等 | 纯净开源，零公司内部依赖 |
| 无前端 | 纯后端服务，无管理界面 | Vue 3 + Element Plus 管理后台 |
| 数据库绑定 | 仅 MySQL | MySQL 主选 + SQLite 轻量模式 |
| 缓存绑定 | 强依赖 Redis | Redis 可选，无 Redis 自动降级 Caffeine |
| RPC 协议 | Dubbo，接入门槛高 | RESTful + OpenFeign，标准 HTTP |
| 部署复杂 | war + Tomcat + Zookeeper | Spring Boot jar 一键启动 |

### 1.2 新版定位

**纯净、轻量、开箱即用的密钥管理系统**，可作为任何项目的安全基础设施。

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 + Vite)                    │
│               Element Plus + Axios + Pinia               │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP/REST
┌─────────────────────▼───────────────────────────────────┐
│              网关层 (Spring Cloud Gateway)                │
│              路由、限流、鉴权、日志                        │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              KMS 核心服务 (Spring Boot 2.7)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ 密钥管理  │ │ 权限管理  │ │ 客户端管理│ │ 审计日志  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ 加密服务  │ │ 限流控制  │ │ 字典管理  │ │ 签名验签  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                   数据层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐   │
│  │  MySQL   │  │  SQLite  │  │ Redis / Caffeine     │   │
│  │ (生产)   │  │ (开发)   │  │ (缓存，可选降级)      │   │
│  └──────────┘  └──────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

```
pcm-kms/
├── pcm-kms-common          # 公共模块：常量、枚举、异常、工具类
├── pcm-kms-core            # 核心业务：加密算法、密钥管理、权限
├── pcm-kms-api             # API 接口定义（DTO、VO、Feign 接口）
├── pcm-kms-server          # 主服务：Controller、配置、启动类
├── pcm-kms-sdk             # 客户端 SDK：注解式加解密 starter
├── pcm-kms-gateway         # 网关（可选）：Spring Cloud Gateway
└── pcm-kms-ui              # 前端：Vue 3 + Element Plus
```

### 2.3 技术选型

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **后端框架** | Spring Boot | 2.7.x | 稳定版，生态成熟 |
| **微服务** | Spring Cloud | 2021.0.x | 服务发现、配置、网关 |
| **注册/配置** | Nacos | 2.x | 替代 Zookeeper + 配置中心 |
| **ORM** | MyBatis-Plus | 3.5.x | 支持 MySQL + SQLite |
| **权限认证** | Sa-Token | 1.37+ | 轻量 RBAC |
| **缓存** | Redis + Caffeine | — | Redis 优先，自动降级 |
| **接口文档** | Knife4j (Swagger) | 4.x | 替代旧版 Swagger |
| **RPC** | OpenFeign + Spring Cloud LoadBalancer | — | 替代 Dubbo |
| **数据库** | MySQL 8.0 / SQLite 3 | — | 双模式 |
| **连接池** | HikariCP | — | Spring Boot 默认 |
| **加密库** | BouncyCastle + Hutool | — | 国密 + 国际算法 |
| **前端框架** | Vue 3 | 3.x | Composition API |
| **UI 组件** | Element Plus | 2.x | 成熟稳定 |
| **构建工具** | Vite | 5.x | 快速开发 |
| **HTTP 客户端** | Axios | 1.x | 前端请求 |
| **状态管理** | Pinia | 2.x | Vue 3 官方推荐 |

---

## 3. 功能设计

### 3.1 功能全景

```
KMS 密钥管理系统
├── 1. 密钥管理（核心）
│   ├── 1.1 密钥生成（RSA/SM2/AES/SM4）
│   ├── 1.2 密钥导入（支持外部密钥录入）
│   ├── 1.3 密钥启用/禁用
│   ├── 1.4 密钥版本管理
│   ├── 1.5 密钥轮转
│   └── 1.6 密钥别名（按应用组隔离）
├── 2. 加密服务
│   ├── 2.1 对称加密/解密（AES、SM4）
│   ├── 2.2 非对称加密/解密（RSA、SM2）
│   ├── 2.3 签名/验签
│   ├── 2.4 摘要（MD5、SM3）
│   └── 2.5 批量加解密
├── 3. 客户端管理
│   ├── 3.1 应用注册（clientId + secret）
│   ├── 3.2 应用组（clientGroup）隔离
│   ├── 3.3 签名验证
│   └── 3.4 密钥授权（哪些应用可用哪些密钥）
├── 4. 权限管理
│   ├── 4.1 用户管理
│   ├── 4.2 角色管理
│   ├── 4.3 菜单/按钮权限
│   └── 4.4 数据权限（按应用组隔离）
├── 5. 限流控制
│   ├── 5.1 按 clientId 限流
│   ├── 5.2 按 API 限流
│   ├── 5.3 限流策略配置
│   └── 5.4 限流日志
├── 6. 审计日志
│   ├── 6.1 操作日志（谁、何时、做了什么）
│   ├── 6.2 密钥访问日志
│   └── 6.3 日志查询与导出
├── 7. 系统管理
│   ├── 7.1 字典管理
│   ├── 7.2 系统参数配置
│   └── 7.3 健康检查
└── 8. SDK（可选）
    ├── 8.1 Java SDK（注解式加解密）
    ├── 8.2 数据库字段自动加解密
    └── 8.3 配置文件加密
```

### 3.2 核心数据模型

#### 密钥基础表 `kms_key_base`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| secret_id | VARCHAR(64) | 密钥唯一ID |
| public_key | TEXT | 公钥（非对称） |
| private_key | TEXT | 私钥（加密存储） |
| secret_key | TEXT | 对称密钥（加密存储） |
| create_time | DATETIME | 创建时间 |
| modify_time | DATETIME | 更新时间 |

#### 密钥元数据表 `kms_key_metadata`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| client_group | VARCHAR(64) | 应用组 |
| secret_id | VARCHAR(64) | 关联密钥ID |
| enabled | TINYINT | 启用状态 |
| algorithm | VARCHAR(32) | 算法：rsa/sm2/aes/sm4/sign/md5/sm3 |
| crypto_type | VARCHAR(32) | 类型：symmetric/asymmetric/sign/digester |
| key_purpose | VARCHAR(64) | 用途：encrypt/decrypt/sign/verify |
| alias | VARCHAR(128) | 别名（应用组内唯一） |
| description | VARCHAR(512) | 描述 |
| key_source | VARCHAR(32) | 来源：system/manual |
| key_version | INT | 版本号 |
| create_time | DATETIME | 创建时间 |
| modify_time | DATETIME | 更新时间 |

#### 客户端信息表 `kms_client_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| client_id | VARCHAR(64) | 客户端唯一ID |
| client_secret | VARCHAR(256) | 客户端密钥（加密存储） |
| client_group | VARCHAR(64) | 所属应用组 |
| client_name | VARCHAR(128) | 客户端名称 |
| enabled | TINYINT | 启用状态 |
| sign_public_key | TEXT | 签名公钥 |

#### 客户端密钥授权表 `kms_client_authorization`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| client_id | VARCHAR(64) | 客户端ID |
| secret_id | VARCHAR(64) | 授权密钥ID |
| enabled | TINYINT | 启用状态 |

### 3.3 加密算法支持

| 算法 | 类型 | 标准 | 用途 |
|------|------|------|------|
| AES-256 | 对称 | 国际 | 数据加解密 |
| SM4 | 对称 | 国密 | 数据加解密 |
| RSA-2048 | 非对称 | 国际 | 密钥交换、签名 |
| SM2 | 非对称 | 国密 | 密钥交换、签名 |
| MD5 | 摘要 | 国际 | 数据完整性 |
| SM3 | 摘要 | 国密 | 数据完整性 |
| SIGN | 签名 | — | 统一签名接口 |

---

## 4. 接口设计

### 4.1 RESTful API 规范

- 基础路径：`/api/v1`
- 统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1718000000000
}
```

### 4.2 核心 API 列表

#### 密钥管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/keys` | 创建密钥 |
| GET | `/api/v1/keys/{id}` | 查看密钥元数据 |
| GET | `/api/v1/keys` | 分页查询密钥列表 |
| PUT | `/api/v1/keys/{id}/enable` | 启用/禁用密钥 |
| DELETE | `/api/v1/keys/{id}` | 删除密钥 |
| POST | `/api/v1/keys/{id}/rotate` | 密钥轮转 |

#### 加密服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/crypto/encrypt` | 加密 |
| POST | `/api/v1/crypto/decrypt` | 解密 |
| POST | `/api/v1/crypto/sign` | 签名 |
| POST | `/api/v1/crypto/verify` | 验签 |
| POST | `/api/v1/crypto/digest` | 摘要 |
| POST | `/api/v1/crypto/batch-encrypt` | 批量加密 |
| GET | `/api/v1/crypto/public-key/{alias}` | 获取公钥 |

#### 客户端管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/clients` | 注册客户端 |
| GET | `/api/v1/clients` | 客户端列表 |
| PUT | `/api/v1/clients/{id}` | 更新客户端 |
| POST | `/api/v1/clients/{id}/authorize` | 授权密钥 |

#### 权限管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/login` | 登录 |
| GET | `/api/v1/users` | 用户列表 |
| POST | `/api/v1/users` | 创建用户 |
| GET | `/api/v1/roles` | 角色列表 |
| POST | `/api/v1/roles` | 创建角色 |

---

## 5. 安全设计

### 5.1 密钥存储安全

- 私钥和对称密钥在数据库中以 **AES-256 主密钥加密** 存储
- 主密钥通过环境变量 `KMS_MASTER_KEY` 注入，不写入配置文件
- 密钥材料（key_base）与元数据（key_metadata）分表存储

### 5.2 传输安全

- 所有 API 建议通过 HTTPS 访问
- 客户端调用需携带签名（clientId + timestamp + nonce + sign）
- 签名算法：HMAC-SHA256(clientSecret, body + timestamp + nonce)

### 5.3 访问控制

- 管理后台：Sa-Token RBAC（用户 → 角色 → 权限）
- API 调用：clientId + 签名验证
- 密钥隔离：按 clientGroup 隔离，应用只能访问被授权的密钥

### 5.4 审计追溯

- 所有密钥操作（创建、查看、加解密、删除）记录审计日志
- 日志包含：操作人/clientId、操作类型、目标密钥、时间、IP、结果

---

## 6. 部署方案

### 6.1 轻量模式（单机开发/小团队）

```
java -jar pcm-kms-server.jar --spring.profiles.active=sqlite
```

- 内嵌 SQLite，无需外部数据库
- 无 Redis，使用 Caffeine 本地缓存
- 前后端一体（后端托管前端静态资源）

### 6.2 标准模式（生产环境）

```
docker-compose up -d
```

- MySQL 8.0 + Redis 7 + Nacos
- 前后端分离部署（Nginx 托管前端）
- 支持水平扩展

### 6.3 完整模式（微服务）

- Spring Cloud Gateway 网关
- Nacos 集群（注册 + 配置）
- KMS 服务多实例
- MySQL 主从 + Redis 哨兵

---

## 7. 与旧版对比

| 维度 | 旧版 (base-kms) | 新版 (pcm-kms) |
|------|----------------|----------------|
| Java 版本 | 8 | 17 |
| 框架 | Spring Boot 2.x + Dubbo | Spring Boot 2.7 + Spring Cloud |
| 注册中心 | Zookeeper | Nacos |
| RPC | Dubbo | REST + OpenFeign |
| 部署 | war + Tomcat | jar 内嵌 |
| 前端 | 无 | Vue 3 + Element Plus |
| 数据库 | 仅 MySQL | MySQL + SQLite |
| 缓存 | 仅 Redis | Redis + Caffeine 降级 |
| 公司依赖 | bajcommon/autopilot/bnt | 零依赖 |
| 接口文档 | Swagger 2 | Knife4j (OpenAPI 3) |
| 加密库 | BouncyCastle | BouncyCastle + Hutool |
| 构建 | Maven | Maven + Vite |

---

## 8. 项目结构总览

```
pcm-kms/
├── docs/                        # 文档
│   ├── design.md                # 本文件：设计方案
│   ├── roadmap.md               # 开发进度周期
│   ├── quickstart.md            # 快速接入文档
│   └── architecture.md          # 框架说明文档
├── pcm-kms-common/              # 公共模块
│   └── src/main/java/com/pcm/kms/common/
├── pcm-kms-core/                # 核心业务模块
│   └── src/main/java/com/pcm/kms/core/
├── pcm-kms-api/                 # API 定义模块
│   └── src/main/java/com/pcm/kms/api/
├── pcm-kms-server/              # 主服务
│   ├── src/main/java/com/pcm/kms/server/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-mysql.yml
│       └── application-sqlite.yml
├── pcm-kms-sdk/                 # 客户端 SDK
│   └── src/main/java/com/pcm/kms/sdk/
├── pcm-kms-gateway/             # 网关（可选）
│   └── src/main/java/com/pcm/kms/gateway/
├── pcm-kms-ui/                  # 前端
│   ├── src/
│   │   ├── views/               # 页面
│   │   ├── components/          # 组件
│   │   ├── api/                 # API 请求
│   │   ├── store/               # Pinia 状态
│   │   └── router/              # 路由
│   ├── package.json
│   └── vite.config.ts
├── sql/                         # 数据库脚本
│   ├── mysql/
│   │   └── init.sql
│   └── sqlite/
│       └── init.sql
├── docker-compose.yml           # Docker 编排
├── pom.xml                      # Maven 父 POM
└── README.md
```
