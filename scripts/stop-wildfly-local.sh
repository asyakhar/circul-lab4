#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="${WILDFLY_HOME:-$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"

stop_server() {
    local name="$1"
    local controller="$2"
    "$WILDFLY_HOME/bin/jboss-cli.sh" \
        --connect \
        --controller="$controller" \
        --command=':shutdown' >/dev/null 2>&1 || true
    echo "$name остановлен."
}

stop_server "Booking WildFly" "127.0.0.1:10190"
stop_server "Ticket WildFly" "127.0.0.1:10090"
