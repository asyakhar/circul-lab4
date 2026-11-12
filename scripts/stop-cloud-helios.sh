#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.run/cloud"

stop_service() {
    local id="$1"
    local pid_file="$RUN_DIR/$id.pid"

    if [[ ! -f "$pid_file" ]]; then
        return
    fi

    local pid
    pid="$(cat "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
        for _ in {1..40}; do
            if ! kill -0 "$pid" 2>/dev/null; then
                break
            fi
            sleep 0.25
        done
    fi
    rm -f "$pid_file"
}

# Stop entry points before their dependencies.
stop_service public-tunnel
stop_service client
stop_service gateway
stop_service haproxy
stop_service booking-2
stop_service booking-1
stop_service ticket
stop_service eureka
stop_service config

echo "Компоненты лабораторной №3 остановлены."
