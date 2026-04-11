#!/bin/bash
# 一键部署到服务器
set -e
cd "$(dirname "$0")"
npm run build
rsync -avz --delete dist/ root@183.56.251.215:/www/wwwroot/183.56.251.215/
echo "✅ 部署完成: http://183.56.251.215/"
