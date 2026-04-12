#!/usr/bin/env bash
# 与 deploy-183.sh 相同，通用命名入口
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$DIR/deploy-183.sh" "$@"
