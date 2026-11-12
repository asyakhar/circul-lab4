#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENABLE_OBSERVABILITY=true exec "$SCRIPT_DIR/start-cloud.sh" "$@"
