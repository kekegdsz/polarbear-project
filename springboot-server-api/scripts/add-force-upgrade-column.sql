-- 为已有 app_versions 表添加 force_upgrade 列
-- 在服务器上执行: mysql -u dg_server -p dg_server < add-force-upgrade-column.sql
-- 或登录 MySQL 后执行:
ALTER TABLE app_versions ADD COLUMN force_upgrade TINYINT DEFAULT 0;
