#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export TICKET_SERVICE_URL="${TICKET_SERVICE_URL:-http://127.0.0.1:8083/api/v1}"
export BOOKING_SERVICE_URL="${BOOKING_SERVICE_URL:-http://127.0.0.1:8082/booking}"

exec "$SCRIPT_DIR/start-client-local.sh"
