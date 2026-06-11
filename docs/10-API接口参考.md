# PCM-KMS API 接口参考

版本：v1.0  
日期：2026-06-11

## 1. 概述

PCM-KMS 提供 RESTful API，分为两大类：

- **管理接口**（`/api/admin/*`）：需要 Sa-Token 登录认证
- **加解密接口**（`/api/crypto/*`）：需要客户端签名认证

公共响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 2. 认证方式

### 管理接口认证

登录获取 Token，后续请求携带 Header：

```
satoken: your-token-value
```

### 加解密接口认证

请求需携带以下 Header：

| Header | 说明 |
|--------|------|
| `X-Client-Id` | 应用客户端ID（启用应用时获得） |
| `X-Timestamp` | 当前时间戳（毫秒） |
| `X-Nonce` | 随机字符串（防重放） |
| `X-Sign` | HMAC-SHA256 签名 |

签名计算方式：

```
sign = HMAC-SHA256(clientSecret, requestBody + timestamp + nonce)
```

开发模式可关闭签名验证：`kms.security.strict-sign=false`

## 3. 认证接口

### 登录

```
POST /api/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "token": "xxx",
    "tokenName": "satoken"
  }
}
```

### 获取当前用户信息

```
GET /api/auth/user-info
```

### 登出

```
POST /api/auth/logout
```

## 4. 应用管理接口

### 创建应用

```
POST /api/admin/apps
```

请求体：

```json
{
  "clientName": "用户服务",
  "clientGroup": "user-center",
  "contacts": "张三",
  "mobile": "13800138000",
  "jobNo": "EMP001",
  "remark": "用户中心密钥接入"
}
```

### 应用列表

```
GET /api/admin/apps
```

### 应用详情

```
GET /api/admin/apps/{id}
```

### 启用应用

```
POST /api/admin/apps/{id}/enable
```

启用后自动生成：
- `clientId`：应用标识
- `clientSecret`：应用密钥
- 签名密钥对
- 默认 AES 密钥

响应中包含接入所需的全部凭证信息。

## 5. 密钥管理接口

### 创建密钥

```
POST /api/admin/keys
```

请求体：

```json
{
  "alias": "user-phone-aes",
  "algorithm": "AES",
  "cryptoType": "SYMMETRIC",
  "keyUsage": "ENCRYPT_DECRYPT",
  "clientGroup": "default",
  "remark": "用户手机号加密"
}
```

algorithm 可选值：

| 值 | 说明 | cryptoType |
|----|------|-----------|
| AES | AES-256-CBC 对称加密 | SYMMETRIC |
| SM4 | SM4 国密对称加密 | SYMMETRIC |
| RSA | RSA-2048 非对称加密 | ASYMMETRIC |
| SM2 | SM2 国密非对称加密 | ASYMMETRIC |
| SIGN | SHA256withRSA 签名 | SIGN |
| MD5 | MD5 摘要 | DIGEST |
| SM3 | SM3 国密摘要 | DIGEST |

### 密钥列表

```
GET /api/admin/keys
```

### 密钥详情

```
GET /api/admin/keys/{id}
```

### 启用密钥

```
POST /api/admin/keys/{id}/enable
```

### 禁用密钥

```
POST /api/admin/keys/{id}/disable
```

### 密钥轮转

```
POST /api/admin/keys/{id}/rotate
```

轮转后：
- 生成新版本密钥材料
- 新版本用于加密
- 旧版本保留解密能力（过渡期兼容）

## 6. 加解密接口

### 加密

```
POST /api/crypto/encrypt
```

请求体：

```json
{
  "alias": "user-phone-aes",
  "plainText": "13800138000",
  "clientGroup": "default"
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "cipherText": "Base64编码的密文",
    "algorithm": "AES/CBC/PKCS5Padding",
    "alias": "user-phone-aes",
    "keyVersion": 1
  }
}
```

### 解密

```
POST /api/crypto/decrypt
```

请求体：

```json
{
  "alias": "user-phone-aes",
  "cipherText": "Base64编码的密文",
  "clientGroup": "default"
}
```

### 签名

```
POST /api/crypto/sign
```

请求体：

```json
{
  "alias": "order-sign-rsa",
  "data": "待签名数据",
  "clientGroup": "default"
}
```

### 验签

```
POST /api/crypto/verify
```

请求体：

```json
{
  "alias": "order-sign-rsa",
  "data": "原始数据",
  "signature": "Base64编码的签名值",
  "clientGroup": "default"
}
```

### 摘要

```
POST /api/crypto/digest
```

请求体：

```json
{
  "plainText": "待计算摘要的数据",
  "algorithm": "SM3"
}
```

algorithm 可选：`MD5`、`SM3`

### 获取公钥

```
GET /api/crypto/public-key/{alias}?clientGroup=default
```

响应：

```json
{
  "code": 200,
  "data": "Base64编码的公钥"
}
```

## 7. 系统接口

### 探活

```
GET /api/system/ping
```

响应：`pong`

## 8. 错误码

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求限流 |
| 500 | 服务内部错误 |

## 9. 限流说明

当启用限流（`kms.ratelimit.enabled=true`）时：

- 每个 IP/clientId 每分钟最多 `kms.ratelimit.max-per-minute` 次请求
- 超出限制返回 HTTP 429

## 10. OpenAPI 文档

启动服务后访问 Swagger UI：http://localhost:8080/doc.html
