-- PCM-KMS 初始化脚本 (SQLite)
-- 版本: V1

CREATE TABLE IF NOT EXISTS kms_client_app (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id TEXT,
    client_secret TEXT,
    client_name TEXT NOT NULL,
    client_group_name TEXT NOT NULL DEFAULT 'default',
    contacts TEXT,
    mobile TEXT,
    job_no TEXT,
    enabled INTEGER NOT NULL DEFAULT 0,
    sign_public_key TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS kms_key_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_group TEXT NOT NULL DEFAULT 'default',
    secret_id TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    algorithm TEXT NOT NULL,
    crypto_type TEXT NOT NULL,
    key_purpose TEXT,
    alias TEXT NOT NULL,
    description TEXT,
    key_source TEXT NOT NULL DEFAULT 'system',
    key_version INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    creator TEXT,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_key_alias_version ON kms_key_metadata(client_group, alias, key_version);

CREATE TABLE IF NOT EXISTS kms_key_material (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    secret_id TEXT NOT NULL,
    public_key TEXT,
    private_key TEXT,
    secret_key TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_key_secret_id ON kms_key_material(secret_id);

CREATE TABLE IF NOT EXISTS kms_client_key_permission (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id TEXT NOT NULL,
    secret_id TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS kms_audit_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operation TEXT NOT NULL,
    operator TEXT,
    resource TEXT,
    resource_id TEXT,
    result TEXT,
    ip TEXT,
    remark TEXT,
    created_at TEXT NOT NULL
);

-- 初始化管理员
CREATE TABLE IF NOT EXISTS kms_user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    nickname TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT
);

INSERT OR IGNORE INTO kms_user (id, username, password, nickname, enabled, created_at)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 1, datetime('now'));
