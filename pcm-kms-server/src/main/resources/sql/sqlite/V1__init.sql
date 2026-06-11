-- ============================================================================
-- PCM-KMS 密钥管理系统 - 初始化脚本 (SQLite)
-- 版本: V1
-- 说明: 包含客户端应用、密钥元数据、密钥材料、授权关系、审计日志、用户 6 张表
-- 注意: SQLite 用于本地开发/单机轻量部署，不支持并发写入，生产环境请用 MySQL
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 客户端应用表：接入 KMS 的业务系统
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_client_app (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , client_id       TEXT                                -- 客户端唯一标识，启用后自动生成，如 kms_a1b2c3d4e5f6g7h8
    , client_secret   TEXT                                -- 客户端密钥，启用后自动生成，用于 HMAC 签名
    , client_name     TEXT NOT NULL                       -- 应用名称，如"订单服务"
    , client_group    TEXT NOT NULL DEFAULT 'default'     -- 应用组，逻辑隔离边界，同一组内密钥互通
    , contacts        TEXT                                -- 联系人姓名
    , mobile          TEXT                                -- 联系人手机号
    , job_no          TEXT                                -- 联系人工号
    , enabled         INTEGER NOT NULL DEFAULT 0          -- 启用状态：0=未启用 1=已启用
    , sign_public_key TEXT                                -- 签名验签公钥（PEM格式），启用后自动生成
    , created_at      TEXT NOT NULL                       -- 创建时间
    , updated_at      TEXT                                -- 最后更新时间
    , deleted         INTEGER NOT NULL DEFAULT 0          -- 逻辑删除：0=未删除，非0=已删除
);

-- ----------------------------------------------------------------------------
-- 密钥元数据表：记录密钥的业务属性（算法、别名、版本等）
-- 不直接存储密钥材料，通过 secret_id 关联 kms_key_material
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_key_metadata (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , client_group    TEXT NOT NULL DEFAULT 'default'     -- 所属应用组
    , secret_id       TEXT NOT NULL                       -- 关联密钥材料ID，如 sid_x1y2z3...
    , enabled         INTEGER NOT NULL DEFAULT 1          -- 启用状态：0=禁用 1=启用
    , algorithm       TEXT NOT NULL                       -- 加密算法：aes/sm4/rsa/sm2/sign/md5/sm3
    , crypto_type     TEXT NOT NULL                       -- 加密类型：symmetric(对称)/asymmetric(非对称)/sign(签名)/digester(摘要)
    , key_purpose     TEXT                                -- 密钥用途：encrypt/decrypt/sign/verify/digest
    , alias           TEXT NOT NULL                       -- 密钥别名，业务系统通过别名引用密钥
    , description     TEXT                                -- 密钥描述/用途说明
    , key_source      TEXT NOT NULL DEFAULT 'system'      -- 密钥来源：system=系统生成 manual=手动导入
    , key_version     INTEGER NOT NULL DEFAULT 1          -- 密钥版本号，轮转后自增
    , created_at      TEXT NOT NULL                       -- 创建时间
    , updated_at      TEXT                                -- 最后更新时间
    , creator         TEXT                                -- 创建人
    , deleted         INTEGER NOT NULL DEFAULT 0          -- 逻辑删除：0=未删除
);

-- 别名+版本唯一索引：同一应用组下别名+版本必须唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_key_alias_version ON kms_key_metadata(client_group, alias, key_version);

-- ----------------------------------------------------------------------------
-- 密钥材料表：存储实际的密钥数据（公钥/私钥/对称密钥）
-- 私钥和对称密钥必须加密存储（使用主密钥 AES-256 加密）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_key_material (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , secret_id       TEXT NOT NULL                       -- 密钥唯一标识，如 sid_x1y2z3...
    , public_key      TEXT                                -- 公钥（PEM/Base64格式），非对称算法才有
    , private_key     TEXT                                -- 私钥（加密存储），非对称算法才有，必须加密后落库
    , secret_key      TEXT                                -- 对称密钥（加密存储），AES/SM4才有，必须加密后落库
    , created_at      TEXT NOT NULL                       -- 创建时间
    , updated_at      TEXT                                -- 最后更新时间
);

-- secret_id 全局唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_key_secret_id ON kms_key_material(secret_id);

-- ----------------------------------------------------------------------------
-- 客户端密钥授权表：控制哪些应用可以访问哪些密钥
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_client_key_permission (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , client_id       TEXT NOT NULL                       -- 客户端ID，关联 kms_client_app.client_id
    , secret_id       TEXT NOT NULL                       -- 密钥ID，关联 kms_key_material.secret_id
    , enabled         INTEGER NOT NULL DEFAULT 1          -- 授权状态：0=已撤销 1=已授权
    , created_at      TEXT NOT NULL                       -- 授权时间
);

-- ----------------------------------------------------------------------------
-- 审计日志表：记录所有密钥操作和 API 调用
-- 不记录明文和完整密文，仅记录操作类型和结果
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_audit_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , operation       TEXT NOT NULL                       -- 操作类型：app_create/key_create/crypto_encrypt 等
    , operator        TEXT                                -- 操作人（管理员用户名或 clientId）
    , resource        TEXT                                -- 资源类型：ClientApp/KeyMetadata/Digest 等
    , resource_id     TEXT                                -- 资源标识（ID 或 secretId）
    , result          TEXT                                -- 操作结果：success/failure
    , ip              TEXT                                -- 请求来源 IP
    , remark          TEXT                                -- 备注信息（如轮转版本变化）
    , created_at      TEXT NOT NULL                       -- 操作时间
);

-- ----------------------------------------------------------------------------
-- 用户表：管理后台登录用户
-- 密码使用 MD5 存储（后续版本升级为 BCrypt）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , username        TEXT NOT NULL                       -- 用户名，唯一
    , password        TEXT NOT NULL                       -- 密码（MD5/BCrypt加密存储）
    , nickname        TEXT                                -- 昵称/显示名
    , enabled         INTEGER NOT NULL DEFAULT 1          -- 启用状态：0=禁用 1=启用
    , created_at      TEXT NOT NULL                       -- 创建时间
    , updated_at      TEXT                                -- 最后更新时间
);

-- 初始管理员账号：admin / 123456
INSERT OR IGNORE INTO kms_user (id, username, password, nickname, enabled, created_at)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 1, datetime('now'));

-- ----------------------------------------------------------------------------
-- 应用限流配置表：每个应用单独的限流配置，覆盖全局默认值
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_app_rate_limit (
    id              INTEGER PRIMARY KEY AUTOINCREMENT  -- 主键
    , client_id       TEXT NOT NULL                       -- 应用 Client ID，关联 kms_client_app.client_id
    , max_per_minute  INTEGER NOT NULL DEFAULT 60         -- 每分钟最大请求数
    , enabled         INTEGER NOT NULL DEFAULT 1          -- 是否启用限流：0=不限 1=限制
    , created_at      TEXT NOT NULL                       -- 创建时间
    , updated_at      TEXT                                -- 最后更新时间
);

-- client_id 唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS uk_app_rate_limit_client ON kms_app_rate_limit(client_id);
