#!/bin/bash
# 一键部署到服务器（需已配置 SSH 免密：ssh root@183.56.251.215）
# 可覆盖：DEPLOY_HOST、DEPLOY_PATH
set -e
cd "$(dirname "$0")"
DEPLOY_HOST="${DEPLOY_HOST:-root@183.56.251.215}"
DEPLOY_PATH="${DEPLOY_PATH:-/www/wwwroot/183.56.251.215}"

npm run build
rsync -avz --delete --exclude '.user.ini' dist/ "${DEPLOY_HOST}:${DEPLOY_PATH}/"
echo "✅ 静态资源已同步 → ${DEPLOY_HOST}:${DEPLOY_PATH}"
echo "   访问: http://183.56.251.215/ （若打不开请查云安全组是否放行 TCP 80）"
