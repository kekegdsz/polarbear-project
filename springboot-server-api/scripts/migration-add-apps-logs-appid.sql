-- MySQL 生产库迁移：新增 apps 表；logs 表增加 app_id 字段

CREATE TABLE IF NOT EXISTS apps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE logs
    ADD COLUMN app_id VARCHAR(64) NOT NULL DEFAULT 'unknown';

CREATE INDEX idx_logs_app_ack_created ON logs (app_id, ack, created_at);

