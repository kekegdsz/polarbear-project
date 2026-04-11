-- 用户表增加昵称（已有库手动执行一次；H2 演示库以 schema.sql 为准）
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(64);

-- MySQL 8.0.12 以下不支持 IF NOT EXISTS 时，可改为：
-- ALTER TABLE users ADD COLUMN nickname VARCHAR(64);

UPDATE users
SET nickname = CASE
    WHEN username IS NOT NULL AND TRIM(username) <> '' THEN username
    ELSE CONCAT('用户', id)
END
WHERE nickname IS NULL OR TRIM(nickname) = '';
