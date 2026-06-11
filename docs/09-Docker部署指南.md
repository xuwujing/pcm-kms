# PCM-KMS Docker 部署指南

版本：v1.0  
日期：2026-06-11

## 1. 部署模式

PCM-KMS 支持两种 Docker 部署模式：

| 模式 | 适用场景 | 依赖 |
|------|---------|------|
| 轻量模式 | 开发/测试/小规模 | SQLite + 本地缓存 |
| 标准模式 | 生产环境 | MySQL + Redis |

## 2. 轻量模式（推荐先试）

### 单容器启动

```bash
# 构建镜像
docker build -f docker/Dockerfile -t pcm-kms:latest .

# 启动（SQLite 模式）
docker run -d \
  --name pcm-kms \
  -p 8080:8080 \
  -e PCM_KMS_MASTER_KEY=change-this-master-key \
  -e SPRING_PROFILES_ACTIVE=sqlite \
  pcm-kms:latest
```

数据持久化：

```bash
docker run -d \
  --name pcm-kms \
  -p 8080:8080 \
  -v /data/pcm-kms:/app/data \
  -e PCM_KMS_MASTER_KEY=change-this-master-key \
  -e SPRING_PROFILES_ACTIVE=sqlite \
  pcm-kms:latest
```

### 包含前端

```bash
docker-compose up -d
```

docker-compose.yml 会启动：
- `pcm-kms-server`：后端服务（8080）
- `pcm-kms-ui`：前端管理后台（80）

## 3. 标准模式（MySQL + Redis）

### docker-compose 标准部署

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: your-root-password
      MYSQL_DATABASE: pcm_kms
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kms-server:
    image: pcm-kms:latest
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"
    environment:
      PCM_KMS_MASTER_KEY: change-this-master-key
      SPRING_PROFILES_ACTIVE: mysql
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/pcm_kms?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: your-root-password
      SPRING_REDIS_HOST: redis

  kms-ui:
    build:
      context: .
      dockerfile: docker/Dockerfile.ui
    ports:
      - "80:80"
    depends_on:
      - kms-server

volumes:
  mysql-data:
```

## 4. 环境变量说明

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `PCM_KMS_MASTER_KEY` | ✅ | — | 主密钥，用于密钥材料加密 |
| `SPRING_PROFILES_ACTIVE` | ❌ | sqlite | 数据库模式：sqlite / mysql |
| `SPRING_DATASOURCE_URL` | MySQL 模式 | — | 数据库连接地址 |
| `SPRING_DATASOURCE_USERNAME` | MySQL 模式 | — | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 模式 | — | 数据库密码 |
| `SPRING_REDIS_HOST` | ❌ | — | Redis 地址 |
| `SPRING_REDIS_PORT` | ❌ | 6379 | Redis 端口 |
| `KMS_SECURITY_STRICT_SIGN` | ❌ | false | 是否强制签名验证 |
| `KMS_RATELIMIT_MAX_PER_MINUTE` | ❌ | 60 | 限流阈值 |

## 5. 生产部署注意事项

### 安全

- **主密钥**：务必修改默认值，建议 32 字节以上随机字符串
- **签名验证**：生产环境建议开启 `KMS_SECURITY_STRICT_SIGN=true`
- **端口暴露**：只暴露必要端口，不要直接暴露 MySQL/Redis
- **HTTPS**：建议在前面加 Nginx/Caddy 做反向代理和 TLS 终结

### 性能

- **JVM 参数**：建议 `-Xms512m -Xmx1g`
- **MySQL**：建议 `max_connections=200`，使用 InnoDB
- **Redis**：建议开启持久化（AOF）

### 备份

- **SQLite**：定期备份 `/app/data/pcm-kms.db`
- **MySQL**：定期 `mysqldump` 或使用 MySQL 主从
- **主密钥**：妥善保管，丢失将无法解密已有密钥材料

## 6. 健康检查

```bash
# 服务存活检查
curl http://localhost:8080/api/system/ping

# Docker 健康检查
docker exec pcm-kms curl -sf http://localhost:8080/api/system/ping
```

## 7. 常见问题

### 容器启动失败

```bash
# 查看日志
docker logs pcm-kms

# 常见原因：
# 1. 端口冲突 → 修改 -p 映射
# 2. 主密钥未设置 → 添加 PCM_KMS_MASTER_KEY 环境变量
# 3. MySQL 未就绪 → 配置 depends_on 或增加启动等待
```

### 前端无法访问后端 API

检查 Nginx 配置（docker/nginx.conf）中的 API 代理地址是否正确指向后端容器。
