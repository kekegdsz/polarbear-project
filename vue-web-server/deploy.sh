#!/usr/bin/env bash
# 仅前端：与仓库根目录 ./deploy.sh --frontend-only 行为一致（路径变量名 DEPLOY_PATH）
# 须与宝塔「网站根目录」一致，否则页面不会变。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
DEPLOY_HOST="${DEPLOY_HOST:-root@183.56.251.215}"
DEPLOY_PATH="${DEPLOY_PATH:-/www/wwwroot/183.56.251.215}"

if [[ ! -d node_modules ]]; then
  npm install
fi
rm -rf dist
npm run build
test -f dist/index.html
GIT_SHA="$(git -C "$ROOT/.." rev-parse --short HEAD 2>/dev/null || echo unknown)"
BUILT_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
printf '{"builtAt":"%s","git":"%s"}\n' "$BUILT_AT" "$GIT_SHA" > dist/polarbear-deploy.json

rsync -avz --delete --exclude '.user.ini' dist/ "${DEPLOY_HOST}:${DEPLOY_PATH}/"
ssh "$DEPLOY_HOST" "cat '${DEPLOY_PATH}/polarbear-deploy.json'"
echo "✅ 静态资源已同步 → ${DEPLOY_HOST}:${DEPLOY_PATH}"
echo "   校验: http://你的域名或IP:端口/polarbear-deploy.json"
echo "   站点监听 8081 或 80 被封时用对应端口；Spring 本机 8082 供 Nginx 反代 /api"
