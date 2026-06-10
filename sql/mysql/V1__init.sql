-- PCM-KMS 初始化脚本 (MySQL)
-- 版本: V1

CREATE TABLE IF NOT EXISTS kms_client_app (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(64),
    client_secret VARCHAR(128),
    client_name VARCHAR(128) NOT NULL,
    client_group_name VARCHAR(64) NOT NULL DEFAULT 'default',
    contacts VARCHAR(64),
    mobile VARCHAR(32),
    job_no VARCHAR(64),
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    sign_public_key TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    deleted BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS kms_key_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_group VARCHAR(64) NOT NULL DEFAULT 'default',
    secret_id VARCHAR(64) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    algorithm VARCHAR(32) NOT NULL,
    crypto_type VARCHAR(32) NOT NULL,
    key_purpose VARCHAR(64),
    alias VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    key_source VARCHAR(32) NOT NULL DEFAULT 'system',
    key_version INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    creator VARCHAR(64),
    deleted BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_alias_version (client_group, alias, key_version)
);

CREATE TABLE IF NOT EXISTS kms_key_material (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    secret_id VARCHAR(64) NOT NULL,
    public_key TEXT,
    private_key TEXT,
    secret_key TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_secret_id (secret_id)
);

CREATE TABLE IF NOT EXISTS kms_client_key_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(64) NOT NULL,
    secret_id VARCHAR(64) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kms_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation VARCHAR(64) NOT NULL,
    operator VARCHAR(64),
    resource VARCHAR(64),
    resource_id VARCHAR(128),
    result VARCHAR(32),
    ip VARCHAR(64),
    remark VARCHAR(512),
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kms_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
);

INSERT IGNORE INTO kms_user (id, username, password, nickname, enabled, created_at)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 1, NOW());
