#!/usr/bin/env bash
# 仓库根目录一键部署入口 → 构建 Vue + 上传静态资源 + 打包上传 Spring Boot 并远程重启
# 用法见 scripts/deploy-183.sh（或执行: DEPLOY_HOST=root@IP ./deploy.sh）
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$ROOT/scripts/deploy-183.sh" "$@"
