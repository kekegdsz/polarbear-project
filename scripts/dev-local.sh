#!/usr/bin/env bash
# 本地同时启动后端 + 前端（改完代码后在本仓库根目录执行）
# 用法: ./scripts/dev-local.sh
# 结束: Ctrl+C（会尝试结束已启动的 Spring Boot）

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/vue-web-server"
if [[ ! -d node_modules ]]; then
  npm install
fi

cd "$ROOT/springboot-server-api"
echo ">>> 启动 Spring Boot (8082，/api + WebSocket)…"
./mvnw -q spring-boot:run &
BACKEND_PID=$!

cleanup() {
  echo ""
  echo ">>> 结束 Spring Boot (pid $BACKEND_PID)"
  kill "$BACKEND_PID" 2>/dev/null || true
  wait "$BACKEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo ">>> 等待后端就绪…"
for i in $(seq 1 90); do
  if curl -sf -o /dev/null "http://127.0.0.1:8082/api/hello"; then
    echo ">>> 后端已就绪"
    break
  fi
  sleep 1
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "后端进程异常退出"
    exit 1
  fi
done

cd "$ROOT/vue-web-server"
echo ">>> 启动 Vite 网页 (http://localhost:8081，/api 代理到 8082)…"
npm run dev
