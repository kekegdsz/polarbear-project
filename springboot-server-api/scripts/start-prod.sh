#!/bin/bash
# 生产环境启动脚本（MySQL 持久化）
# 使用前：在宝塔面板设置 MySQL root 密码为 PA4HKdcDZ7jRkj99，或修改下方 MYSQL_PASSWORD

set -e
cd "$(dirname "$0")"

MYSQL_PASSWORD="${MYSQL_PASSWORD:-3DcMYF2ar8A4Ee8P}"
WAR_NAME="springboot-server-api-0.0.1-SNAPSHOT.war"
JAVA_BIN="/www/server/java/jdk-17.0.8/bin/java"

# 数据库 dg_server 需已在 MySQL 中创建

# 停止旧进程
pkill -f springboot-server-api 2>/dev/null || true
sleep 2

# 启动
export MYSQL_PASSWORD
nohup "$JAVA_BIN" -jar "$WAR_NAME" \
  --spring.profiles.active=prod \
  --server.port=8082 \
  > app.log 2>&1 &

echo "已启动，日志: tail -f app.log"
