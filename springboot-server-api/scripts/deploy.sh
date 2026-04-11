#!/bin/bash
# 本地打包 + 上传到服务器脚本
# 使用方法: ./deploy.sh user@server_ip

set -e

PROJECT_NAME="springboot-server-api"
WAR_NAME="${PROJECT_NAME}-0.0.1-SNAPSHOT.war"
REMOTE_DIR="/opt/apps/${PROJECT_NAME}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ">>> 1. 打包项目..."
cd "$PROJECT_DIR"
./mvnw clean package -DskipTests -q

if [ ! -f "target/${WAR_NAME}" ]; then
  echo "打包失败，未找到 target/${WAR_NAME}"
  exit 1
fi

echo ">>> 2. 打包完成: target/${WAR_NAME}"

if [ -n "$1" ]; then
  REMOTE="$1"
  echo ">>> 3. 上传到服务器 ${REMOTE}..."
  ssh "$REMOTE" "mkdir -p ${REMOTE_DIR}"
  scp "target/${WAR_NAME}" "${REMOTE}:${REMOTE_DIR}/"
  scp "scripts/start-prod.sh" "${REMOTE}:${REMOTE_DIR}/" 2>/dev/null || true
  scp "scripts/springboot-api.service" "${REMOTE}:/tmp/" 2>/dev/null || true
  ssh "$REMOTE" "chmod +x ${REMOTE_DIR}/start-prod.sh 2>/dev/null || true"
  echo ">>> 4. 上传完成"
  echo ""
  echo "MySQL 持久化启动（需先在宝塔设置 MySQL root 密码）："
  echo "  ssh $REMOTE 'cd ${REMOTE_DIR} && MYSQL_PASSWORD=你的密码 ./start-prod.sh'"
  echo ""
  echo "或使用 dev（H2 内存库）："
  echo "  ssh $REMOTE 'cd ${REMOTE_DIR} && nohup /www/server/java/jdk-17.0.8/bin/java -jar ${WAR_NAME} --spring.profiles.active=dev --server.port=8082 > app.log 2>&1 &'"
else
  echo ""
  echo "未指定服务器，仅完成打包。"
  echo "上传并部署请执行: $0 user@你的服务器IP"
fi
