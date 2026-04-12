-- 管理后台「系统发信」使用的虚拟用户（与 application.yml im.system-sender-user-id 一致，默认 999）
-- MySQL / 生产库在已有 users 表上执行一次即可；H2 开发库已由 schema.sql 初始化插入。

INSERT INTO users (id, device_uuid, username, nickname, password, role, created_at, updated_at)
SELECT 999, 'im-system', 'im_system', '系统通知', 'x', 'admin', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 999);

-- 已存在旧数据时，刷新展示名与登录名（便于客户端展示发件人）
UPDATE users
SET username = 'im_system', nickname = '系统通知', device_uuid = 'im-system', role = 'admin', updated_at = NOW()
WHERE id = 999 AND (nickname IS NULL OR nickname IN ('系统', 'system') OR username = 'system');
