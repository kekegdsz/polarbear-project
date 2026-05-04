#!/bin/bash
# 服务器上启动 Spring Boot WAR（与 deploy-183.sh / deploy.sh 配套）
#
# 环境变量：
#   MYSQL_PASSWORD   prod 时必填（与 application-prod.yml 中库密码一致）
#   SPRING_PROFILE   默认 prod；无 MySQL 可设 dev（H2 内存，仅演示）
#   JAVA_BIN         可选，显式指定 java 路径
#   SERVER_PORT      默认 8082（与文档中 Nginx 反代一致）

set -e
cd "$(dirname "$0")"

WAR_NAME="springboot-server-api-0.0.1-SNAPSHOT.war"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
SERVER_PORT="${SERVER_PORT:-8082}"

if [[ ! -f "$WAR_NAME" ]]; then
  echo "错误: 当前目录无 $WAR_NAME" >&2
  exit 1
fi

# 解析 java：JAVA_BIN → JAVA_HOME → 宝塔常见路径 → PATH
JAVA_CMD="${JAVA_BIN:-}"
if [[ -z "$JAVA_CMD" || ! -x "$JAVA_CMD" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    JAVA_CMD="${JAVA_HOME}/bin/java"
  elif [[ -x "/www/server/java/jdk-17.0.8/bin/java" ]]; then
    JAVA_CMD="/www/server/java/jdk-17.0.8/bin/java"
  elif [[ -x "/usr/bin/java" ]]; then
    JAVA_CMD="/usr/bin/java"
  else
    JAVA_CMD="$(command -v java || true)"
  fi
fi
if [[ -z "$JAVA_CMD" ]]; then
  echo "错误: 找不到 java，请安装 JDK17 或设置 JAVA_HOME / JAVA_BIN" >&2
  exit 1
fi

echo "使用 Java: $JAVA_CMD"
"$JAVA_CMD" -version 2>&1 | head -1

# 停止旧进程（按 war 文件名匹配；避免与宝塔「Java 项目管理器」重复启动时请只保留一处）
pkill -f "${WAR_NAME}" 2>/dev/null || true
sleep 2

# 空字符串会覆盖 yml 中 ${MYSQL_PASSWORD:默认}，表现为「无密码」连库导致启动失败
if [[ -z "${MYSQL_PASSWORD:-}" ]]; then
  unset MYSQL_PASSWORD
else
  export MYSQL_PASSWORD
fi

echo "启动 profile=$SPRING_PROFILE  port=$SERVER_PORT ..."
nohup "$JAVA_CMD" -jar "$WAR_NAME" \
  --spring.profiles.active="$SPRING_PROFILE" \
  --server.port="$SERVER_PORT" \
  > app.log 2>&1 &

if ! pgrep -f "$WAR_NAME" >/dev/null 2>&1; then
  echo "警告: 未检测到进程，请查看 app.log" >&2
  tail -40 app.log >&2 || true
  exit 1
fi

echo "等待 /api/hello 就绪（prod 冷启动可能需数十秒，避免 Nginx 502）..."
READY_URL="http://127.0.0.1:${SERVER_PORT}/api/hello"
for _ in $(seq 1 90); do
  if curl -sf "$READY_URL" >/dev/null 2>&1; then
    echo "已就绪。日志: tail -f $(pwd)/app.log"
    echo "本机探测: curl -s ${READY_URL}"
    exit 0
  fi
  sleep 2
done

echo "错误: ${READY_URL} 在约 3 分钟内仍未响应（Nginx 可能出现 502）。请查看 app.log" >&2
tail -50 app.log >&2 || true
exit 1
