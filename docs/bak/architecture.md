# PCM-KMS 框架说明文档

> 后端：Spring Boot 2.7 + Spring Cloud 2021 + MyBatis-Plus 3.5
> 前端：Vue 3 + Vite + Element Plus
> 版本：v1.0 | 日期：2026-06-10

---

## 目录

1. [项目简介](#1-项目简介)
2. [后端技术栈详解](#2-后端技术栈详解)
3. [前端技术栈详解](#3-前端技术栈详解)
4. [项目结构](#4-项目结构)
5. [快速开始](#5-快速开始)
6. [配置说明](#6-配置说明)
7. [开发指南](#7-开发指南)
8. [部署指南](#8-部署指南)
9. [API 文档](#9-api-文档)
10. [贡献指南](#10-贡献指南)

---

## 1. 项目简介

**PCM-KMS**（Key Management System）是一个轻量级、开箱即用的密钥管理系统。

### 特性

- 🔐 **多算法支持**：AES、RSA、SM2、SM4、MD5、SM3、签名验签
- 🧩 **双数据库模式**：MySQL（生产）+ SQLite（开发/单机），自动切换
- 📦 **缓存降级**：Redis 优先，不可用时自动降级为 Caffeine 本地缓存
- 🔑 **多租户隔离**：按应用组（clientGroup）隔离密钥，客户端签名鉴权
- 🛡️ **安全防护**：密钥加密存储、传输签名、限流控制、审计日志
- 🎨 **管理后台**：Vue 3 + Element Plus，开箱即用
- 🚀 **一键启动**：SQLite 模式零依赖，`java -jar` 即可运行
- 🐳 **Docker 支持**：docker-compose 一键部署全家桶
- 📋 **SDK 接入**：Java Starter，注解式加解密，5 分钟集成

---

## 2. 后端技术栈详解

### 2.1 核心框架

| 框架 | 版本 | 用途 | 官网 |
|------|------|------|------|
| Spring Boot | 2.7.x | 应用框架 | https://spring.io/projects/spring-boot |
| Spring Cloud | 2021.0.x | 微服务治理 | https://spring.io/projects/spring-cloud |
| Spring Cloud Gateway | 3.1.x | API 网关（可选） | https://spring.io/projects/spring-cloud-gateway |
| Spring Cloud OpenFeign | 3.1.x | 声明式 HTTP 客户端 | https://spring.io/projects/spring-cloud-openfeign |

> **为什么选 Spring Boot 2.7 而不是 3.x？**
> 2.7 是 2.x 系列最后一个稳定大版本，生态兼容性最好。后续可平滑升级到 3.x（需 JDK 17 → 无影响，我们已用 JDK 17）。

### 2.2 数据访问

| 框架 | 版本 | 用途 |
|------|------|------|
| MyBatis-Plus | 3.5.x | ORM，支持 MySQL + SQLite 自动切换 |
| HikariCP | — | 数据库连接池（Spring Boot 默认） |
| MySQL Connector | 8.0.x | MySQL 驱动 |
| SQLite JDBC | 3.x | SQLite 驱动 |

**多数据源切换原理**：

```java
// 通过 spring.profiles.active 切换
// application-sqlite.yml → 加载 SQLite 驱动
// application-mysql.yml → 加载 MySQL 驱动
// MyBatis-Plus 自动适配，上层代码无需改动
```

### 2.3 缓存

| 框架 | 版本 | 用途 |
|------|------|------|
| Spring Data Redis | — | Redis 集成 |
| Caffeine | 2.9.x | 本地缓存（Redis 降级方案） |

**缓存降级策略**：

```java
@Configuration
public class CacheConfig {
    @Bean
    @ConditionalOnProperty(name = "spring.redis.host")
    public CacheManager redisCacheManager() {
        // Redis 可用时使用 Redis
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisCacheManager")
    public CacheManager caffeineCacheManager() {
        // Redis 不可用时降级为 Caffeine
    }
}
```

### 2.4 安全与认证

| 框架 | 版本 | 用途 |
|------|------|------|
| Sa-Token | 1.37+ | 轻量级 RBAC 权限认证 |
| BouncyCastle | 1.79 | 国密算法（SM2/SM3/SM4） |
| Hutool Crypto | 5.8.x | 加密工具库 |

**为什么选 Sa-Token 而不是 Spring Security？**
- Sa-Token 更轻量，API 更直观
- 天然支持注解鉴权、Token 会话、踢人下线
- 比 Spring Security 学习成本低 80%

### 2.5 注册与配置中心

| 框架 | 版本 | 用途 |
|------|------|------|
| Nacos | 2.x | 服务注册 + 配置中心 |

> Nacos 为可选依赖。设置 `spring.cloud.nacos.discovery.enabled=false` 即可单机运行。

### 2.6 接口文档

| 框架 | 版本 | 用途 |
|------|------|------|
| Knife4j | 4.x | Swagger 增强 UI |
| SpringDoc OpenAPI | 1.6.x | OpenAPI 3 规范 |

访问地址：`http://localhost:8080/doc.html`

### 2.7 工具库

| 框架 | 版本 | 用途 |
|------|------|------|
| Lombok | 1.18.x | 简化 POJO |
| MapStruct | 1.5.x | 对象转换 |
| Hutool | 5.8.x | 通用工具集 |
| Jackson | 2.13.x | JSON 序列化 |
| Hibernate Validator | — | 参数校验 |

---

## 3. 前端技术栈详解

### 3.1 核心框架

| 框架 | 版本 | 用途 | 官网 |
|------|------|------|------|
| Vue | 3.x | 渐进式框架 | https://vuejs.org |
| Vite | 5.x | 构建工具 | https://vitejs.dev |
| TypeScript | 5.x | 类型安全 | https://www.typescriptlang.org |

### 3.2 UI 与组件

| 框架 | 版本 | 用途 |
|------|------|------|
| Element Plus | 2.x | UI 组件库 |
| @element-plus/icons-vue | — | 图标库 |

### 3.3 状态与路由

| 框架 | 版本 | 用途 |
|------|------|------|
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理（Vue 3 官方推荐） |

### 3.4 网络请求

| 框架 | 版本 | 用途 |
|------|------|------|
| Axios | 1.x | HTTP 客户端 |

### 3.5 其他

| 框架 | 版本 | 用途 |
|------|------|------|
| crypto-js | 4.x | 前端签名计算 |
| dayjs | 1.x | 日期处理 |

---

## 4. 项目结构

```
pcm-kms/
│
├── docs/                              # 📚 项目文档
│   ├── design.md                      #   设计方案
│   ├── roadmap.md                     #   开发进度周期
│   ├── quickstart.md                  #   快速接入文档
│   └── architecture.md                #   本文件：框架说明
│
├── pcm-kms-common/                    # 🧱 公共模块
│   └── src/main/java/com/pcm/kms/common/
│       ├── constant/                  #   常量定义
│       ├── enums/                     #   枚举（算法类型、加密类型等）
│       ├── exception/                 #   异常定义
│       ├── result/                    #   统一响应体（R<T>）
│       └── utils/                     #   工具类
│
├── pcm-kms-core/                      # 💼 核心业务模块
│   └── src/main/java/com/pcm/kms/core/
│       ├── crypto/                    #   加密算法实现
│       │   ├── strategy/              #     策略模式：AES/SM4/RSA/SM2/MD5/SM3/Sign
│       │   └── CryptoService.java     #     加密服务门面
│       ├── entity/                    #   数据库实体（DO）
│       ├── mapper/                    #   MyBatis-Plus Mapper
│       ├── manager/                   #   数据访问管理层
│       ├── service/                   #   业务服务接口
│       │   └── impl/                  #     业务服务实现
│       └── config/                    #   核心模块配置
│
├── pcm-kms-api/                       # 📡 API 定义模块
│   └── src/main/java/com/pcm/kms/api/
│       ├── dto/                       #   数据传输对象
│       ├── vo/                        #   视图对象
│       └── feign/                     #   OpenFeign 接口（供其他微服务调用）
│
├── pcm-kms-server/                    # 🚀 主服务（启动入口）
│   └── src/main/
│       ├── java/com/pcm/kms/server/
│       │   ├── controller/            #   REST Controller
│       │   ├── config/                #   Web 配置、CORS、线程池
│       │   ├── filter/                #   过滤器（签名验证、限流）
│       │   ├── interceptor/           #   拦截器（权限、日志）
│       │   ├── handler/               #   全局异常处理
│       │   └── KmsServerApplication.java  # 启动类
│       └── resources/
│           ├── application.yml        #   主配置
│           ├── application-mysql.yml  #   MySQL 环境配置
│           └── application-sqlite.yml #   SQLite 环境配置
│
├── pcm-kms-sdk/                       # 🧰 Java 客户端 SDK
│   └── src/main/java/com/pcm/kms/sdk/
│       ├── annotation/                #   @KmsEncryptField / @KmsDecryptField
│       ├── aspect/                    #   AOP 切面（自动加解密）
│       ├── autoconfigure/             #   Spring Boot 自动配置
│       ├── interceptor/               #   MyBatis TypeHandler
│       ├── service/                   #   KmsCryptoService（编程式 API）
│       └── config/                    #   SDK 配置属性
│
├── pcm-kms-gateway/                   # 🌐 API 网关（可选）
│   └── src/main/java/com/pcm/kms/gateway/
│       ├── filter/                    #   网关过滤器
│       └── GatewayApplication.java
│
├── pcm-kms-ui/                        # 🎨 前端管理后台
│   ├── src/
│   │   ├── api/                       #   API 请求封装
│   │   ├── assets/                    #   静态资源
│   │   ├── components/                #   公共组件
│   │   ├── composables/               #   Vue 3 组合式函数
│   │   ├── layout/                    #   布局组件
│   │   ├── router/                    #   路由配置
│   │   ├── store/                     #   Pinia 状态管理
│   │   ├── utils/                     #   工具函数
│   │   ├── views/                     #   页面视图
│   │   │   ├── dashboard/             #     首页仪表盘
│   │   │   ├── keys/                  #     密钥管理
│   │   │   ├── crypto/                #     加解密测试
│   │   │   ├── clients/               #     客户端管理
│   │   │   ├── system/                #     系统管理（用户/角色/权限）
│   │   │   ├── monitor/               #     监控（限流/日志）
│   │   │   └── login/                 #     登录
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── sql/                               # 🗄️ 数据库脚本
│   ├── mysql/
│   │   └── init.sql                   #   MySQL 初始化
│   └── sqlite/
│       └── init.sql                   #   SQLite 初始化
│
├── docker/                            # 🐳 Docker 相关
│   ├── Dockerfile.server
│   ├── Dockerfile.ui
│   └── nginx.conf
│
├── docker-compose.yml                 # 一键部署编排
├── pom.xml                            # Maven 父 POM
├── README.md                          # 项目首页
├── LICENSE                            # 开源协议
└── .gitignore
```

---

## 5. 快速开始

### 5.1 环境准备

```bash
# 必需
JDK 17+
Maven 3.6+
Node.js 18+
pnpm (npm install -g pnpm)

# 可选（生产模式）
MySQL 8.0+
Redis 7.0+
```

### 5.2 后端启动

```bash
# 克隆
git clone https://github.com/your-org/pcm-kms.git
cd pcm-kms

# 编译
mvn clean package -DskipTests

# SQLite 模式（零依赖）
cd pcm-kms-server
java -jar target/pcm-kms-server.jar --spring.profiles.active=sqlite

# MySQL 模式
java -jar target/pcm-kms-server.jar --spring.profiles.active=mysql
```

### 5.3 前端启动

```bash
cd pcm-kms-ui
pnpm install
pnpm dev
```

浏览器打开 http://localhost:5173

### 5.4 Docker 启动

```bash
docker-compose up -d
```

---

## 6. 配置说明

### 6.1 后端核心配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: pcm-kms-server
  profiles:
    active: sqlite    # sqlite | mysql

# Sa-Token 配置
sa-token:
  token-name: satoken
  timeout: 2592000          # Token 有效期（秒），默认 30 天
  is-concurrent: true       # 是否允许同一账号并发登录
  token-style: uuid         # Token 风格

# KMS 主密钥（环境变量注入，不要写在配置文件里）
# export KMS_MASTER_KEY=your-256-bit-master-key
```

### 6.2 MySQL 模式配置

```yaml
# application-mysql.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pcm_kms?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: ${REDIS_PASSWORD:}
```

### 6.3 SQLite 模式配置

```yaml
# application-sqlite.yml
spring:
  datasource:
    url: jdbc:sqlite:./data/kms.db
    driver-class-name: org.sqlite.JDBC
  redis:
    host:                    # 留空，触发 Caffeine 降级
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

### 6.4 前端配置

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 6.5 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `KMS_MASTER_KEY` | 密钥加密主密钥（32 字节） | 首次启动自动生成 |
| `MYSQL_PASSWORD` | MySQL 密码 | root |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `NACOS_SERVER_ADDR` | Nacos 地址 | 127.0.0.1:8848 |

---

## 7. 开发指南

### 7.1 添加新的加密算法

1. 在 `pcm-kms-common/enums/AlgorithmEnum.java` 添加枚举值
2. 在 `pcm-kms-core/crypto/strategy/` 创建实现类，实现 `AlgorithmStrategy` 接口
3. Bean 命名规则：`{算法code}AlgorithmStrategy`，如 `sm4AlgorithmStrategy`
4. 无需修改其他代码，策略模式自动发现

```java
@Component("sm4AlgorithmStrategy")
public class Sm4AlgorithmStrategy implements AlgorithmStrategy {
    @Override
    public String encrypt(String plainText, String key) { ... }

    @Override
    public String decrypt(String cipherText, String key) { ... }

    @Override
    public AlgorithmEnum getAlgorithm() {
        return AlgorithmEnum.SM4;
    }
}
```

### 7.2 添加新的 REST 接口

```java
@RestController
@RequestMapping("/api/v1/demo")
@Api(tags = "示例接口")
public class DemoController {

    @GetMapping
    @ApiOperation("示例查询")
    @SaCheckPermission("demo:query")   // 权限注解
    public R<Page<DemoVO>> list(DemoQueryDTO query) {
        return R.success(demoService.page(query));
    }

    @PostMapping
    @ApiOperation("示例新增")
    @LogAnnotation(operation = "DEMO_ADD")  // 操作日志注解
    public R<DemoVO> create(@Valid @RequestBody DemoAddDTO dto) {
        return R.success(demoService.create(dto));
    }
}
```

### 7.3 前端添加新页面

1. `src/views/demo/index.vue` — 页面组件
2. `src/api/demo.ts` — API 请求
3. `src/router/index.ts` — 添加路由
4. `src/store/modules/demo.ts` — 状态管理（如需要）

### 7.4 代码规范

- **后端**：遵循阿里巴巴 Java 开发手册
- **前端**：ESLint + Prettier，Vue 3 Composition API
- **提交**：Conventional Commits（`feat:` / `fix:` / `docs:` / `refactor:`）

---

## 8. 部署指南

### 8.1 单机部署

```bash
# 后端
nohup java -jar pcm-kms-server.jar --spring.profiles.active=mysql > kms.log 2>&1 &

# 前端（Nginx 托管）
cp -r pcm-kms-ui/dist/* /usr/share/nginx/html/
```

### 8.2 Docker Compose 部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: pcm_kms
    volumes:
      - ./sql/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine

  kms-server:
    build: ./docker
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: mysql
      MYSQL_PASSWORD: root123
      REDIS_HOST: redis
      KMS_MASTER_KEY: ${KMS_MASTER_KEY}
    depends_on:
      - mysql
      - redis

  kms-ui:
    build:
      context: ./pcm-kms-ui
      dockerfile: ../docker/Dockerfile.ui
    ports:
      - "80:80"
    depends_on:
      - kms-server
```

### 8.3 Kubernetes 部署

```bash
kubectl apply -f k8s/
```

---

## 9. API 文档

启动后访问：**http://localhost:8080/doc.html**

Knife4j 自动生成 OpenAPI 3 规范文档，支持在线调试。

主要接口分组：

| 分组 | 路径前缀 | 说明 |
|------|---------|------|
| 密钥管理 | `/api/v1/keys` | 密钥 CRUD、轮转 |
| 加密服务 | `/api/v1/crypto` | 加解密、签名、摘要 |
| 客户端管理 | `/api/v1/clients` | 客户端注册、授权 |
| 用户管理 | `/api/v1/users` | 用户 CRUD |
| 角色管理 | `/api/v1/roles` | 角色 CRUD |
| 权限管理 | `/api/v1/permissions` | 权限树 |
| 限流管理 | `/api/v1/rate-limit` | 限流策略配置 |
| 审计日志 | `/api/v1/logs` | 操作日志查询 |
| 字典管理 | `/api/v1/dicts` | 系统字典 |
| 认证 | `/api/v1/auth` | 登录、登出 |

---

## 10. 贡献指南

### 分支管理

- `main` — 稳定发布分支
- `dev` — 开发分支
- `feature/*` — 功能分支
- `fix/*` — 修复分支

### 提交流程

```bash
git checkout -b feature/my-feature
# ... 开发 ...
git commit -m "feat: 添加密钥轮转功能"
git push origin feature/my-feature
# 创建 Pull Request → dev → main
```

### 联系方式

- Issues: https://github.com/your-org/pcm-kms/issues
- 作者: pcm

---

## 附录：依赖版本清单

### Maven 父 POM 关键依赖

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>2.7.18</spring-boot.version>
    <spring-cloud.version>2021.0.9</spring-cloud.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <sa-token.version>1.37.0</sa-token.version>
    <hutool.version>5.8.30</hutool.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <knife4j.version>4.3.0</knife4j.version>
    <bouncycastle.version>1.79</bouncycastle.version>
    <caffeine.version>2.9.3</caffeine.version>
</properties>
```

### 前端 package.json 关键依赖

```json
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "axios": "^1.7.0",
    "element-plus": "^2.7.0",
    "@element-plus/icons-vue": "^2.3.0",
    "crypto-js": "^4.2.0",
    "dayjs": "^1.11.0"
  },
  "devDependencies": {
    "vite": "^5.2.0",
    "typescript": "^5.4.0",
    "@vitejs/plugin-vue": "^5.0.0",
    "unplugin-auto-import": "^0.17.0",
    "unplugin-vue-components": "^0.26.0"
  }
}
```
