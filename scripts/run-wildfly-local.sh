#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"
TLS_DIR="$LAB2_DIR/wildfly/tls"

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21)}"
export JAVA_HOME

TICKET_PID=""
BOOKING_PID=""

stop_processes() {
    [[ -n "$BOOKING_PID" ]] && kill "$BOOKING_PID" 2>/dev/null || true
    [[ -n "$TICKET_PID" ]] && kill "$TICKET_PID" 2>/dev/null || true
}
trap stop_processes EXIT INT TERM

wait_for_server() {
    local name="$1"
    local log_file="$2"
    local pid="$3"

    for _ in {1..90}; do
        if grep -q 'WFLYSRV0025' "$log_file" 2>/dev/null; then
            echo "$name готов."
            return 0
        fi
        if grep -q 'WFLYSRV0026' "$log_file" 2>/dev/null; then
            echo "$name запустился с ошибками." >&2
            tail -80 "$log_file" >&2
            return 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name неожиданно завершился." >&2
            tail -80 "$log_file" >&2
            return 1
        fi
        sleep 1
    done

    echo "$name не успел запуститься" >&2
    return 1
}

TICKET_BASE="$SERVERS_DIR/ticket"
BOOKING_BASE="$SERVERS_DIR/booking"

: >"$TICKET_BASE/log/server.log"
: >"$TICKET_BASE/log/console.log"
"$WILDFLY_HOME/bin/standalone.sh" \
    -Djboss.server.base.dir="$TICKET_BASE" \
    -Djboss.socket.binding.port-offset=100 \
    -Djboss.tx.node.id=ticket \
    >"$TICKET_BASE/log/console.log" 2>&1 &
TICKET_PID=$!
wait_for_server "Ticket WildFly" "$TICKET_BASE/log/server.log" "$TICKET_PID"

: >"$BOOKING_BASE/log/server.log"
: >"$BOOKING_BASE/log/console.log"
"$WILDFLY_HOME/bin/standalone.sh" \
    -Djboss.server.base.dir="$BOOKING_BASE" \
    -Djboss.socket.binding.port-offset=200 \
    -Djboss.tx.node.id=booking \
    -Dticket.service.url=https://localhost:8543/api/v1 \
    -Djavax.net.ssl.trustStore="$TLS_DIR/booking-truststore.p12" \
    -Djavax.net.ssl.trustStorePassword=changeit \
    >"$BOOKING_BASE/log/console.log" 2>&1 &
BOOKING_PID=$!
wait_for_server "Booking WildFly" "$BOOKING_BASE/log/server.log" "$BOOKING_PID"

echo "Ticket Service:  https://localhost:8543/api/v1"
echo "Booking Service: https://localhost:8643/booking"
echo "все запущено"

wait "$TICKET_PID" "$BOOKING_PID"
