#!/usr/bin/env bash
# 一键通过 SSH 部署：Vue 静态站 + Spring Boot WAR 到 root@183.56.251.215
#
# 依赖：本机已配置 SSH 免密登录目标机；本机有 node、npm、mvn 或项目内 ./mvnw
#
# 环境变量（可选）：
#   DEPLOY_HOST=root@IP
#   DEPLOY_WEB_ROOT=/www/wwwroot/183.56.251.215   # 宝塔站点根目录
#   DEPLOY_REMOTE_APP=/opt/apps/springboot-server-api
#   SPRING_PROFILE=prod|dev   默认 prod（需 MySQL）；无库时可 SPRING_PROFILE=dev
#   MYSQL_PASSWORD=...        prod 时与服务器 MySQL 一致
#
set -euo pipefail

DEPLOY_HOST="${DEPLOY_HOST:-root@183.56.251.215}"
DEPLOY_WEB_ROOT="${DEPLOY_WEB_ROOT:-/www/wwwroot/183.56.251.215}"
DEPLOY_REMOTE_APP="${DEPLOY_REMOTE_APP:-/opt/apps/springboot-server-api}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo ">>> 项目根: $ROOT"
echo ">>> 目标: $DEPLOY_HOST"
echo ">>> 前端目录: $DEPLOY_WEB_ROOT"
echo ">>> 后端目录: $DEPLOY_REMOTE_APP (profile=$SPRING_PROFILE)"
echo ""

echo ">>> [1/4] 构建 Vue..."
cd "$ROOT/vue-web-server"
if [[ ! -d node_modules ]]; then
  npm install
fi
npm run build

echo ">>> [2/4] 同步静态资源 (rsync)..."
ssh "$DEPLOY_HOST" "mkdir -p '$DEPLOY_WEB_ROOT'"
rsync -avz --delete --exclude '.user.ini' dist/ "${DEPLOY_HOST}:${DEPLOY_WEB_ROOT}/"
echo "    前端已同步"

echo ">>> [3/4] 打包并上传 Spring Boot WAR..."
cd "$ROOT/springboot-server-api"
if [[ -x ./mvnw ]]; then
  ./mvnw clean package -DskipTests -q
else
  mvn clean package -DskipTests -q
fi
WAR="target/springboot-server-api-0.0.1-SNAPSHOT.war"
if [[ ! -f "$WAR" ]]; then
  echo "错误: 未找到 $WAR"
  exit 1
fi
ssh "$DEPLOY_HOST" "mkdir -p '$DEPLOY_REMOTE_APP'"
scp "$WAR" "${DEPLOY_HOST}:${DEPLOY_REMOTE_APP}/"
scp "$ROOT/springboot-server-api/scripts/start-prod.sh" "${DEPLOY_HOST}:${DEPLOY_REMOTE_APP}/"
ssh "$DEPLOY_HOST" "chmod +x '${DEPLOY_REMOTE_APP}/start-prod.sh'"
echo "    后端已上传"

echo ">>> [4/4] 远程重启 Java..."
ssh "$DEPLOY_HOST" bash <<SSHEOF
export SPRING_PROFILE="${SPRING_PROFILE}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
cd "${DEPLOY_REMOTE_APP}"
./start-prod.sh
SSHEOF

IP="${DEPLOY_HOST#*@}"
echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo "  官网:     http://${IP}/"
echo "  API 文档: http://${IP}/api-doc"
echo "  IM 测试:  http://${IP}/im-chat.html"
echo "  健康检查: http://${IP}/api/hello  （需 Nginx 将 /api 反代到本机 8082）"
echo "  IM WS:    ws://${IP}/api/im/ws    （同上，需 Upgrade）"
echo ""
echo "若 /api 502：SSH 上执行 curl -s http://127.0.0.1:8082/api/hello"
echo "生产库部署: SPRING_PROFILE=prod MYSQL_PASSWORD='你的密码' $0"
echo "=========================================="
