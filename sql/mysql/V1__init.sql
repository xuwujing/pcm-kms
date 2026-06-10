-- ============================================================================
-- PCM-KMS 密钥管理系统 - 初始化脚本 (MySQL)
-- 版本: V1
-- 说明: 包含客户端应用、密钥元数据、密钥材料、授权关系、审计日志、用户 6 张表
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 客户端应用表：接入 KMS 的业务系统
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_client_app (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    client_id       VARCHAR(64)                        COMMENT '客户端唯一标识，启用后自动生成，如 kms_a1b2c3d4e5f6g7h8',
    client_secret   VARCHAR(128)                       COMMENT '客户端密钥，启用后自动生成，用于 HMAC 签名',
    client_name     VARCHAR(128) NOT NULL              COMMENT '应用名称，如"订单服务"',
    client_group_name VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '应用组，逻辑隔离边界，同一组内密钥互通',
    contacts        VARCHAR(64)                        COMMENT '联系人姓名',
    mobile          VARCHAR(32)                        COMMENT '联系人手机号',
    job_no          VARCHAR(64)                        COMMENT '联系人工号',
    enabled         TINYINT(1) NOT NULL DEFAULT 0      COMMENT '启用状态：0=未启用 1=已启用',
    sign_public_key TEXT                               COMMENT '签名验签公钥（PEM格式），启用后自动生成',
    created_at      DATETIME NOT NULL                  COMMENT '创建时间',
    updated_at      DATETIME                           COMMENT '最后更新时间',
    deleted         BIGINT NOT NULL DEFAULT 0          COMMENT '逻辑删除：0=未删除，非0=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端应用表-接入KMS的业务系统';

-- ----------------------------------------------------------------------------
-- 密钥元数据表：记录密钥的业务属性（算法、别名、版本等）
-- 不直接存储密钥材料，通过 secret_id 关联 kms_key_material
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_key_metadata (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    client_group    VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '所属应用组',
    secret_id       VARCHAR(64) NOT NULL               COMMENT '关联密钥材料ID，如 sid_x1y2z3...',
    enabled         TINYINT(1) NOT NULL DEFAULT 1      COMMENT '启用状态：0=禁用 1=启用',
    algorithm       VARCHAR(32) NOT NULL               COMMENT '加密算法：aes/sm4/rsa/sm2/sign/md5/sm3',
    crypto_type     VARCHAR(32) NOT NULL               COMMENT '加密类型：symmetric(对称)/asymmetric(非对称)/sign(签名)/digester(摘要)',
    key_purpose     VARCHAR(64)                        COMMENT '密钥用途：encrypt/decrypt/sign/verify/digest',
    alias           VARCHAR(128) NOT NULL              COMMENT '密钥别名，业务系统通过别名引用密钥，如 my-app-db-pwd',
    description     VARCHAR(512)                       COMMENT '密钥描述/用途说明',
    key_source      VARCHAR(32) NOT NULL DEFAULT 'system' COMMENT '密钥来源：system=系统生成 manual=手动导入',
    key_version     INT NOT NULL DEFAULT 1             COMMENT '密钥版本号，轮转后自增',
    created_at      DATETIME NOT NULL                  COMMENT '创建时间',
    updated_at      DATETIME                           COMMENT '最后更新时间',
    creator         VARCHAR(64)                        COMMENT '创建人',
    deleted         BIGINT NOT NULL DEFAULT 0          COMMENT '逻辑删除：0=未删除',
    UNIQUE KEY uk_alias_version (client_group, alias, key_version) COMMENT '同一应用组下别名+版本唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥元数据表-密钥的业务属性和版本管理';

-- ----------------------------------------------------------------------------
-- 密钥材料表：存储实际的密钥数据（公钥/私钥/对称密钥）
-- 私钥和对称密钥必须加密存储（使用主密钥 AES-256 加密）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_key_material (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    secret_id       VARCHAR(64) NOT NULL               COMMENT '密钥唯一标识，如 sid_x1y2z3...',
    public_key      TEXT                               COMMENT '公钥（PEM/Base64格式），非对称算法才有',
    private_key     TEXT                               COMMENT '私钥（加密存储），非对称算法才有，必须加密后落库',
    secret_key      TEXT                               COMMENT '对称密钥（加密存储），AES/SM4才有，必须加密后落库',
    created_at      DATETIME NOT NULL                  COMMENT '创建时间',
    updated_at      DATETIME                           COMMENT '最后更新时间',
    UNIQUE KEY uk_secret_id (secret_id)                COMMENT 'secret_id 全局唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密钥材料表-公钥/私钥/对称密钥的加密存储';

-- ----------------------------------------------------------------------------
-- 客户端密钥授权表：控制哪些应用可以访问哪些密钥
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_client_key_permission (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    client_id       VARCHAR(64) NOT NULL               COMMENT '客户端ID，关联 kms_client_app.client_id',
    secret_id       VARCHAR(64) NOT NULL               COMMENT '密钥ID，关联 kms_key_material.secret_id',
    enabled         TINYINT(1) NOT NULL DEFAULT 1      COMMENT '授权状态：0=已撤销 1=已授权',
    created_at      DATETIME NOT NULL                  COMMENT '授权时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端密钥授权表-应用与密钥的访问控制';

-- ----------------------------------------------------------------------------
-- 审计日志表：记录所有密钥操作和 API 调用
-- 不记录明文和完整密文，仅记录操作类型和结果
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_audit_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    operation       VARCHAR(64) NOT NULL               COMMENT '操作类型：app_create/key_create/crypto_encrypt 等',
    operator        VARCHAR(64)                        COMMENT '操作人（管理员用户名或 clientId）',
    resource        VARCHAR(64)                        COMMENT '资源类型：ClientApp/KeyMetadata/Digest 等',
    resource_id     VARCHAR(128)                       COMMENT '资源标识（ID 或 secretId）',
    result          VARCHAR(32)                        COMMENT '操作结果：success/failure',
    ip              VARCHAR(64)                        COMMENT '请求来源 IP',
    remark          VARCHAR(512)                       COMMENT '备注信息（如轮转版本变化）',
    created_at      DATETIME NOT NULL                  COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表-密钥操作和API调用的追溯记录';

-- ----------------------------------------------------------------------------
-- 用户表：管理后台登录用户
-- 密码使用 MD5 存储（后续版本升级为 BCrypt）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kms_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT  COMMENT '主键',
    username        VARCHAR(64) NOT NULL               COMMENT '用户名，唯一',
    password        VARCHAR(128) NOT NULL              COMMENT '密码（MD5/BCrypt加密存储）',
    nickname        VARCHAR(64)                        COMMENT '昵称/显示名',
    enabled         TINYINT(1) NOT NULL DEFAULT 1      COMMENT '启用状态：0=禁用 1=启用',
    created_at      DATETIME NOT NULL                  COMMENT '创建时间',
    updated_at      DATETIME                           COMMENT '最后更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表-管理后台登录账号';

-- 初始管理员账号：admin / 123456
INSERT IGNORE INTO kms_user (id, username, password, nickname, enabled, created_at)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 1, NOW());
