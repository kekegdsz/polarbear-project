-- MySQL / 生产库按需执行：Gradle 编译上报表
CREATE TABLE IF NOT EXISTS compile_build_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id VARCHAR(64) NOT NULL,
    project_key VARCHAR(128),
    machine VARCHAR(512),
    os_user VARCHAR(128),
    duration_ms BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    tasks VARCHAR(1024),
    success TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_compile_build_logs_app_created (app_id, created_at)
);
