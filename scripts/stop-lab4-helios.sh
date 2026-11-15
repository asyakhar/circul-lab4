#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.run/lab4-helios"
WILDFLY_HOME="${WILDFLY_HOME:-$PROJECT_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
WILDFLY_TICKET_BASE="${WILDFLY_TICKET_BASE:-$PROJECT_DIR/wildfly/servers/ticket-lab4}"
MULE_HOME="${MULE_HOME:-$PROJECT_DIR/mule/runtime}"
TICKET_MANAGEMENT_PORT="${TICKET_MANAGEMENT_PORT:-19577}"

stop_pid_file() {
    local pid_file="$1"
    if [[ ! -f "$pid_file" ]]; then
        return
    fi

    local pid
    pid="$(cat "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
        for _ in {1..40}; do
            kill -0 "$pid" 2>/dev/null || break
            sleep 0.25
        done
    fi
    rm -f "$pid_file"
}

stop_pid_file "$RUN_DIR/client.pid"
stop_pid_file "$RUN_DIR/booking.pid"

if [[ -x "$MULE_HOME/bin/mule" ]]; then
    "$MULE_HOME/bin/mule" stop >/dev/null 2>&1 || true
fi
stop_pid_file "$RUN_DIR/mule.pid"

if [[ -x "$WILDFLY_HOME/bin/jboss-cli.sh" ]]; then
    "$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
        --controller="127.0.0.1:$TICKET_MANAGEMENT_PORT" \
        --command=':shutdown' >/dev/null 2>&1 || true
fi
stop_pid_file "$RUN_DIR/ticket.pid"

while IFS= read -r pid; do
    [[ -n "$pid" ]] && kill "$pid" 2>/dev/null || true
done < <(pgrep -u "$(id -u)" -f -- \
    "-Djboss.server.base.dir=$WILDFLY_TICKET_BASE" || true)

echo "Компоненты лабораторной №4 на Helios остановлены."
