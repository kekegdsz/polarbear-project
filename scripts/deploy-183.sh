#!/usr/bin/env bash
# =============================================================================
# 一键部署：本机构建 Vue + rsync 静态资源 + 打包 Spring Boot WAR + SSH 上传并重启
#
# 快捷执行（项目根目录）：
#   chmod +x deploy.sh && ./deploy.sh
#
# 依赖：本机 node、npm；本机 ./springboot-server-api/mvnw 或 mvn；ssh / scp / rsync；
#       目标机已配置 SSH 公钥免密登录。
#
# 环境变量（可选）：
#   DEPLOY_HOST=root@IP
#   DEPLOY_WEB_ROOT=/www/wwwroot/站点目录      # Nginx 网站根（须与宝塔「网站根目录」完全一致）
#   DEPLOY_PATH=...                            # 同 DEPLOY_WEB_ROOT，与 vue-web-server/deploy.sh 一致
#   DEPLOY_REMOTE_APP=/opt/apps/springboot-server-api
#   SPRING_PROFILE=prod|dev   默认 prod（需 MySQL）
#   MYSQL_PASSWORD=...        prod 时在远程传给 start-prod.sh
#   SPRING_SERVER_PORT=8082   远程 Spring 监听端口（与 Nginx 反代一致）
#   PUBLIC_HTTP_PORT=8081     仅用于部署完成后的提示文案
#
# 示例：
#   DEPLOY_HOST=root@1.2.3.4 DEPLOY_WEB_ROOT=/www/wwwroot/myweb ./deploy.sh
#   SPRING_PROFILE=prod MYSQL_PASSWORD='secret' ./deploy.sh
#
# 仅更新前端（后台页面不生效时优先试这个，并核对 DEPLOY_WEB_ROOT）：
#   ./deploy.sh --frontend-only
#
# =============================================================================
set -euo pipefail

usage() {
  sed -n '2,27p' "$0" | sed 's/^# \{0,1\}//'
  echo "参数:"
  echo "  --frontend-only   只构建并 rsync 前端，跳过 WAR 与远程 Java 重启"
  echo "  -h, --help        显示说明"
  exit 0
}

FRONTEND_ONLY=0
ARGS=()
for a in "$@"; do
  case "$a" in
    --frontend-only) FRONTEND_ONLY=1 ;;
    -h|--help) usage ;;
    *) ARGS+=("$a") ;;
  esac
done
if [[ ${#ARGS[@]} -gt 0 ]]; then
  echo "错误: 未知参数: ${ARGS[*]}"
  exit 1
fi

command -v ssh >/dev/null || { echo "错误: 需要 ssh"; exit 1; }
command -v rsync >/dev/null || { echo "错误: 需要 rsync"; exit 1; }
command -v scp >/dev/null || { echo "错误: 需要 scp"; exit 1; }
command -v node >/dev/null || { echo "错误: 需要 node（构建 Vue）"; exit 1; }
command -v npm >/dev/null || { echo "错误: 需要 npm"; exit 1; }

DEPLOY_HOST="${DEPLOY_HOST:-root@183.56.251.215}"
# DEPLOY_PATH 与 vue-web-server/deploy.sh 一致，便于同一套环境变量
DEPLOY_WEB_ROOT="${DEPLOY_WEB_ROOT:-${DEPLOY_PATH:-/www/wwwroot/183.56.251.215}}"
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

STEP_TOTAL=4
if [[ "$FRONTEND_ONLY" -eq 1 ]]; then
  STEP_TOTAL=2
fi
echo ">>> [1/${STEP_TOTAL}] 构建 Vue（每次清空 dist，避免旧产物残留）..."
cd "$ROOT/vue-web-server"
if [[ ! -d node_modules ]]; then
  npm install
fi
rm -rf dist
npm run build
if [[ ! -f dist/index.html ]]; then
  echo "错误: dist/index.html 不存在，构建失败"
  exit 1
fi
GIT_SHA="$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"
BUILT_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
printf '{"builtAt":"%s","git":"%s"}\n' "$BUILT_AT" "$GIT_SHA" > dist/polarbear-deploy.json
echo "    构建标记: $BUILT_AT  $GIT_SHA"

echo ">>> [2/${STEP_TOTAL}] 同步静态资源 → ${DEPLOY_HOST}:${DEPLOY_WEB_ROOT}/"
echo "    （宝塔面板「网站」→ 站点 → 根目录 必须与上面路径一致，否则页面不会变）"
ssh "$DEPLOY_HOST" "mkdir -p '$DEPLOY_WEB_ROOT'"
rsync -avz --delete --exclude '.user.ini' dist/ "${DEPLOY_HOST}:${DEPLOY_WEB_ROOT}/"
echo "    前端已同步"

echo ">>> 远程校验（应能看到刚生成的 polarbear-deploy.json）："
ssh "$DEPLOY_HOST" "set -e; test -f '${DEPLOY_WEB_ROOT}/index.html' || { echo '错误: 远程缺少 index.html，检查 DEPLOY_WEB_ROOT'; exit 1; }; ls -la '${DEPLOY_WEB_ROOT}/index.html' '${DEPLOY_WEB_ROOT}/polarbear-deploy.json'; echo '---'; cat '${DEPLOY_WEB_ROOT}/polarbear-deploy.json'"

if [[ "$FRONTEND_ONLY" -eq 1 ]]; then
  echo ""
  echo "已跳过后端部署。若后台接口仍是旧行为，请去掉 --frontend-only 再部署一次。"
else
echo ">>> [3/${STEP_TOTAL}] 打包并上传 Spring Boot WAR..."
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

echo ">>> [4/${STEP_TOTAL}] 远程重启 Java..."
# 勿向远端 export 空的 MYSQL_PASSWORD，否则会盖住 application-prod.yml 里的默认占位符
if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  ssh "$DEPLOY_HOST" bash -s <<SSHEOF
export MYSQL_PASSWORD=$(printf '%q' "$MYSQL_PASSWORD")
export SPRING_PROFILE="${SPRING_PROFILE}"
export SERVER_PORT="${SPRING_SERVER_PORT}"
cd "${DEPLOY_REMOTE_APP}"
./start-prod.sh
SSHEOF
else
  ssh "$DEPLOY_HOST" bash <<SSHEOF
export SPRING_PROFILE="${SPRING_PROFILE}"
export SERVER_PORT="${SPRING_SERVER_PORT}"
cd "${DEPLOY_REMOTE_APP}"
./start-prod.sh
SSHEOF
fi
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
echo "生产库 SQL（按需执行 resources/db/ 下）："
echo "  migration-users-nickname.sql、migration-im-group-member-role.sql、"
echo "  migration-im-messages-body-mediumtext.sql、migration-admin-im-system-user.sql（后台系统发信用户 999）"
echo "Nginx 需 client_max_body_size ≥100m 以便 /api/im/upload"
echo "生产部署示例: SPRING_PROFILE=prod MYSQL_PASSWORD='你的密码' $(basename "$0")"
echo "自定义主机: DEPLOY_HOST=root@IP DEPLOY_WEB_ROOT=/www/wwwroot/xxx $(basename "$0")"
echo "前端是否已更新：浏览器打开 http://${IP}:${P}/polarbear-deploy.json 应对应当次构建时间；"
echo "若仍为旧内容，多半是 Nginx 根目录不是 ${DEPLOY_WEB_ROOT}，或 CDN/浏览器强缓存 index.html（可试无痕窗口）。"
echo "=========================================="
