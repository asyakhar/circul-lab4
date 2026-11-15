#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BOOKING_JAR="$PROJECT_DIR/booking-service/build/libs/booking-service-1.0.0.jar"
RUN_DIR="$PROJECT_DIR/.run/lab4"

if [[ ! -r "$BOOKING_JAR" ]]; then
    echo "Booking Service не собран. Выполните ./scripts/build-lab4.sh." >&2
    exit 1
fi

mkdir -p "$RUN_DIR"

export TICKET_SERVICE_URL="${TICKET_SERVICE_URL:-http://127.0.0.1:8083/api/v1}"
export BOOKING_SERVICE_PORT="${BOOKING_SERVICE_PORT:-8082}"
export BOOKING_STORE_PATH="${BOOKING_STORE_PATH:-$RUN_DIR/booking.db}"

echo "Booking Service: http://127.0.0.1:$BOOKING_SERVICE_PORT/booking"
echo "Ticket REST API: $TICKET_SERVICE_URL"

exec java -jar "$BOOKING_JAR"
