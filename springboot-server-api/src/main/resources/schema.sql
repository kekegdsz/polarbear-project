-- 版本发布表（渠道：android/ios/ohos）
CREATE TABLE IF NOT EXISTS app_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_name VARCHAR(32) NOT NULL,
    version_code INT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    download_url VARCHAR(512),
    release_notes TEXT,
    force_upgrade TINYINT DEFAULT 0,
    published TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- 用户表（支持设备 UUID 自动注册，账号密码可选）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_uuid VARCHAR(64) UNIQUE,
    username VARCHAR(50) UNIQUE,
    nickname VARCHAR(64),
    password VARCHAR(100),
    mobile VARCHAR(20),
    token VARCHAR(64),
    token_expire_time TIMESTAMP,
    vip VARCHAR(20) DEFAULT 'normal',
    vip_expire_time TIMESTAMP,
    role VARCHAR(20) DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表（支持微信/支付宝）
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_type VARCHAR(20),
    payment_trade_no VARCHAR(128),
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 应用表（用于区分哪个应用上传的日志）
CREATE TABLE IF NOT EXISTS apps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(128) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 日志表（管理后台）
CREATE TABLE IF NOT EXISTS logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    app_id VARCHAR(64) NOT NULL,
    employee_no VARCHAR(64),
    duration_ms BIGINT,
    content VARCHAR(4000) NOT NULL,
    ack TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Gradle / CI 编译上报（管理后台可查看）
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========== 即时通讯（Netty WebSocket）==========
CREATE TABLE IF NOT EXISTS im_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS im_group_members (
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS im_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_type VARCHAR(8) NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT,
    group_id BIGINT,
    body CLOB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 测试用户（IM 演示用，内存库每次启动为空可直接插入）
INSERT INTO users (id, device_uuid, username, nickname, password, role)
VALUES (101, 'im-demo-101', 'alice', 'alice', 'x', 'user');
INSERT INTO users (id, device_uuid, username, nickname, password, role)
VALUES (102, 'im-demo-102', 'bob', 'bob', 'x', 'user');
INSERT INTO users (id, device_uuid, username, nickname, password, role)
VALUES (103, 'im-demo-103', 'carol', 'carol', 'x', 'user');
INSERT INTO users (id, device_uuid, username, nickname, password, role)
VALUES (999, 'im-system', 'im_system', '系统通知', 'x', 'admin');
ALTER TABLE users ALTER COLUMN id RESTART WITH 104;
