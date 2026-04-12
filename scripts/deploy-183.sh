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
# 云厂商常屏蔽 80，站点若监听 8081，对外请用 http://IP:8081/
PUBLIC_HTTP_PORT="${PUBLIC_HTTP_PORT:-8081}"
SPRING_SERVER_PORT="${SPRING_SERVER_PORT:-8082}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
echo ">>> 项目根: $ROOT"
echo ">>> 目标: $DEPLOY_HOST"
echo ">>> 前端目录: $DEPLOY_WEB_ROOT"
echo ">>> 后端目录: $DEPLOY_REMOTE_APP (profile=$SPRING_PROFILE)"
echo ">>> 对外 HTTP 端口: $PUBLIC_HTTP_PORT（Nginx）；Spring 本机: $SPRING_SERVER_PORT"
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
# 勿向远端 export 空的 MYSQL_PASSWORD，否则会盖住 application-prod.yml 里的默认占位符
if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  ssh "$DEPLOY_HOST" bash -s <<SSHEOF
export MYSQL_PASSWORD=$(printf '%q' "$MYSQL_PASSWORD")
export SPRING_PROFILE="${SPRING_PROFILE}"
cd "${DEPLOY_REMOTE_APP}"
./start-prod.sh
SSHEOF
else
  ssh "$DEPLOY_HOST" bash <<SSHEOF
export SPRING_PROFILE="${SPRING_PROFILE}"
cd "${DEPLOY_REMOTE_APP}"
./start-prod.sh
SSHEOF
fi

IP="${DEPLOY_HOST#*@}"
P="${PUBLIC_HTTP_PORT}"
S="${SPRING_SERVER_PORT}"
echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo "  （若 80 被封，请用下面带 :${P} 的地址）"
echo "  官网:     http://${IP}:${P}/"
echo "  API 文档: http://${IP}:${P}/api-doc"
echo "  IM 测试:  http://${IP}:${P}/im-chat.html"
echo "  健康检查: http://${IP}:${P}/api/hello"
echo "  IM WS:    ws://${IP}:${P}/api/im/ws"
echo ""
echo "  直连 Spring（仅当安全组放行 ${S}）: http://${IP}:${S}/api/hello"
echo ""
echo "若 :${P}/api 502：SSH 上执行 curl -s http://127.0.0.1:${S}/api/hello"
echo "生产库补列（按需）：db/migration-users-nickname.sql、db/migration-im-group-member-role.sql、db/migration-im-messages-body-mediumtext.sql（IM 附件 JSON）"
echo "Nginx 需 client_max_body_size ≥100m 以便 /api/im/upload"
echo "生产库部署: SPRING_PROFILE=prod MYSQL_PASSWORD='你的密码' $0"
echo "=========================================="
