#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

WILDFLY_HOME="${WILDFLY_HOME:-$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"
TLS_DIR="$LAB2_DIR/wildfly/tls"

if [[ -z "${JAVA_HOME:-}" ]]; then
    if [[ "$(uname -s)" == "Darwin" ]]; then
        JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    else
        JAVA_PATH="$(readlink -f "$(command -v java)")"
        JAVA_HOME="$(dirname "$(dirname "$JAVA_PATH")")"
    fi
fi

export JAVA_HOME

if [[ ! -x "$WILDFLY_HOME/bin/standalone.sh" ]]; then
    echo "WildFly не найден: $WILDFLY_HOME" >&2
    exit 1
fi

wait_for_start() {
    local name="$1"
    local base="$2"
    local pid="$3"

    for _ in {1..90}; do
        if grep -q 'WFLYSRV0025' "$base/log/server.log" 2>/dev/null; then
            echo "$name запущен (PID $pid)."
            return 0
        fi

        if grep -q 'WFLYSRV0026' "$base/log/server.log" 2>/dev/null; then
            echo "$name запустился с ошибками:" >&2
            tail -80 "$base/log/server.log" >&2
            return 1
        fi

        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name завершился с ошибкой:" >&2
            tail -80 "$base/log/console.log" >&2
            return 1
        fi

        sleep 1
    done

    echo "$name не успел запуститься за 90 секунд." >&2
    return 1
}

start_ticket() {
    local base="$SERVERS_DIR/ticket"

    : >"$base/log/server.log"
    : >"$base/log/console.log"

    nohup "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.server.base.dir="$base" \
        -Djboss.socket.binding.port-offset=100 \
        -Djboss.tx.node.id=ticket \
        >"$base/log/console.log" 2>&1 &

    local pid=$!
    echo "$pid" >"$base/server.pid"

    wait_for_start "Ticket WildFly" "$base" "$pid"
}

prepare_truststore() {
    local ticket_base="$SERVERS_DIR/ticket"

    mkdir -p "$TLS_DIR"

    curl -sk https://localhost:8543/ >/dev/null || true

    keytool -exportcert -rfc \
        -alias server \
        -keystore "$ticket_base/configuration/application.keystore" \
        -storepass password \
        -file "$TLS_DIR/ticket-service.crt" \
        >/dev/null

    rm -f "$TLS_DIR/booking-truststore.p12"

    keytool -importcert -noprompt \
        -alias ticket-service \
        -file "$TLS_DIR/ticket-service.crt" \
        -keystore "$TLS_DIR/booking-truststore.p12" \
        -storetype PKCS12 \
        -storepass changeit \
        >/dev/null

    echo "Truststore для Booking Service подготовлен."
}

start_booking() {
    local base="$SERVERS_DIR/booking"

    : >"$base/log/server.log"
    : >"$base/log/console.log"

    nohup "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.server.base.dir="$base" \
        -Djboss.socket.binding.port-offset=200 \
        -Djboss.tx.node.id=booking \
        -Dticket.service.url=https://localhost:8543/api/v1 \
        -Djavax.net.ssl.trustStore="$TLS_DIR/booking-truststore.p12" \
        -Djavax.net.ssl.trustStorePassword=changeit \
        >"$base/log/console.log" 2>&1 &

    local pid=$!
    echo "$pid" >"$base/server.pid"

    wait_for_start "Booking WildFly" "$base" "$pid"
}

start_ticket
prepare_truststore
start_booking

echo "Ticket Service:  https://localhost:8543/api/v1"
echo "Booking Service: https://localhost:8643/booking"