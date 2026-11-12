#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
SERVERS_DIR="$LAB2_DIR/wildfly/servers"
TLS_DIR="$LAB2_DIR/wildfly/tls"
CLIENT_DIR="$LAB2_DIR/client"
CUSTOM_CLIENT_CERT="$CLIENT_DIR/tls/frontend.crt"
CUSTOM_CLIENT_KEY="$CLIENT_DIR/tls/frontend.key"
GENERATED_CLIENT_CERT="$TLS_DIR/client-cert.pem"
GENERATED_CLIENT_KEY="$TLS_DIR/client-key.pem"
CLIENT_CERT_FILE=""
CLIENT_KEY_FILE=""
TICKET_OFFSET=9587
BOOKING_OFFSET=9687
TICKET_PORT=18030
BOOKING_PORT=18130
CLIENT_PORT=18230

COMMON_JAVA_OPTS="-Xms32m -Xss256k -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Dorg.jboss.weld.bootstrap.concurrentDeployment=false --add-opens=java.base/java.util=ALL-UNNAMED"
export JAVA_OPTS="$COMMON_JAVA_OPTS -Xmx160m -XX:MaxMetaspaceSize=144m"

port_is_busy() {
    local port="$1"
    (echo >/dev/tcp/127.0.0.1/"$port") >/dev/null 2>&1
}

check_ports() {
    local busy=()
    local port

    for port in 19577 19677 "$TICKET_PORT" "$BOOKING_PORT" "$CLIENT_PORT"; do
        if port_is_busy "$port"; then
            busy+=("$port")
        fi
    done

    if ((${#busy[@]} > 0)); then
        echo "Нельзя запустить сервисы: порты уже заняты: ${busy[*]}" >&2
        echo "Вероятно, экземпляр WildFly уже запущен." >&2
        echo "Выполните ./scripts/stop-helios.sh, затем повторите запуск." >&2
        exit 1
    fi
}

prepare_client_certificate() {
    if [[ -e "$CUSTOM_CLIENT_CERT" || -e "$CUSTOM_CLIENT_KEY" ]]; then
        if [[ ! -r "$CUSTOM_CLIENT_CERT" || ! -r "$CUSTOM_CLIENT_KEY" ]]; then
            echo "Пользовательский TLS-сертификат и ключ должны существовать вместе:" >&2
            echo "  $CUSTOM_CLIENT_CERT" >&2
            echo "  $CUSTOM_CLIENT_KEY" >&2
            exit 1
        fi
        CLIENT_CERT_FILE="$CUSTOM_CLIENT_CERT"
        CLIENT_KEY_FILE="$CUSTOM_CLIENT_KEY"
        echo "Используется пользовательский HTTPS-сертификат клиента."
        return
    fi

    bash "$SCRIPT_DIR/setup-client-tls.sh"
    CLIENT_CERT_FILE="$GENERATED_CLIENT_CERT"
    CLIENT_KEY_FILE="$GENERATED_CLIENT_KEY"

    if [[ ! -r "$CLIENT_CERT_FILE" || ! -r "$CLIENT_KEY_FILE" ]]; then
        echo "Не удалось подготовить HTTPS-сертификат клиента." >&2
        exit 1
    fi
}

wait_for_server() {
    local name="$1"
    local base="$2"
    local pid="$3"

    for _ in {1..90}; do
        if grep -q 'WFLYSRV0025' "$base/log/server.log" 2>/dev/null; then
            echo "$name запущен (PID $pid)."
            return
        fi
        if grep -q 'WFLYSRV0026' "$base/log/server.log" 2>/dev/null; then
            echo "$name запустился с ошибками." >&2
            tail -60 "$base/log/server.log" >&2
            exit 1
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name завершился с ошибкой." >&2
            tail -60 "$base/log/console.log" >&2
            exit 1
        fi
        sleep 1
    done

    echo "$name не успел запуститься." >&2
    exit 1
}

wait_for_client() {
    local pid="$1"

    for _ in {1..30}; do
        if curl -ksS --fail --max-time 2 \
            "https://127.0.0.1:$CLIENT_PORT/" >/dev/null 2>&1; then
            echo "HTTPS-клиент запущен (PID $pid)."
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "HTTPS-клиент завершился с ошибкой." >&2
            tail -60 "$CLIENT_DIR/client.log" >&2
            exit 1
        fi
        sleep 1
    done

    echo "HTTPS-клиент не успел запуститься." >&2
    tail -60 "$CLIENT_DIR/client.log" >&2
    exit 1
}

start_wildfly() {
    local name="$1"
    local base="$2"
    local offset="$3"
    shift 3

    : >"$base/log/server.log"
    : >"$base/log/console.log"
    nohup "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.server.base.dir="$base" \
        -Djboss.socket.binding.port-offset="$offset" \
        -Djboss.bind.address=127.0.0.1 \
        -Djboss.bind.address.management=127.0.0.1 \
        "$@" >"$base/log/console.log" 2>&1 &

    local pid=$!
    echo "$pid" >"$base/server.pid"
    wait_for_server "$name" "$base" "$pid"
}

prepare_client_certificate
check_ports

start_wildfly "Ticket WildFly" "$SERVERS_DIR/ticket" "$TICKET_OFFSET" \
    -Djboss.tx.node.id=ticket

mkdir -p "$TLS_DIR"
curl -sk "https://127.0.0.1:$TICKET_PORT/" >/dev/null
keytool -J-Xmx128m -exportcert -rfc -alias server \
    -keystore "$SERVERS_DIR/ticket/configuration/application.keystore" \
    -storepass password -file "$TLS_DIR/ticket-service.crt" >/dev/null

rm -f "$TLS_DIR/booking-truststore.p12"
keytool -J-Xmx128m -importcert -noprompt -alias ticket-service \
    -file "$TLS_DIR/ticket-service.crt" \
    -keystore "$TLS_DIR/booking-truststore.p12" \
    -storetype PKCS12 -storepass changeit >/dev/null

export JAVA_OPTS="$COMMON_JAVA_OPTS -Xmx128m -XX:MaxMetaspaceSize=128m"
start_wildfly "Booking WildFly" "$SERVERS_DIR/booking" "$BOOKING_OFFSET" \
    -Djboss.tx.node.id=booking \
    -Dticket.service.url="https://localhost:$TICKET_PORT/api/v1" \
    -Djavax.net.ssl.trustStore="$TLS_DIR/booking-truststore.p12" \
    -Djavax.net.ssl.trustStorePassword=changeit

if [[ ! -x "$CLIENT_DIR/.venv/bin/python" ]]; then
    python3.11 -m venv "$CLIENT_DIR/.venv"
fi
"$CLIENT_DIR/.venv/bin/python" -m pip install --quiet \
    -r "$CLIENT_DIR/requirements.txt"

nohup env \
    TICKET_SERVICE_URL="https://127.0.0.1:$TICKET_PORT/api/v1" \
    BOOKING_SERVICE_URL="https://127.0.0.1:$BOOKING_PORT/booking" \
    CLIENT_PORT="$CLIENT_PORT" \
    CLIENT_CERT_FILE="$CLIENT_CERT_FILE" \
    CLIENT_KEY_FILE="$CLIENT_KEY_FILE" \
    "$CLIENT_DIR/.venv/bin/python" "$CLIENT_DIR/app.py" \
    >"$CLIENT_DIR/client.log" 2>&1 &
CLIENT_PID=$!
echo "$CLIENT_PID" >"$CLIENT_DIR/client.pid"
wait_for_client "$CLIENT_PID"

echo "Ticket Service:  https://127.0.0.1:$TICKET_PORT/api/v1"
echo "Booking Service: https://127.0.0.1:$BOOKING_PORT/booking"
echo "Client:          https://127.0.0.1:$CLIENT_PORT"
