#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin:$PATH"
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.run/lab4-helios"
WILDFLY_HOME="${WILDFLY_HOME:-$PROJECT_DIR/wildfly/wildfly-ee-10-41.0.1.Final}"
WILDFLY_TICKET_BASE="${WILDFLY_TICKET_BASE:-$PROJECT_DIR/wildfly/servers/ticket-lab4}"
MULE_HOME="${MULE_HOME:-$PROJECT_DIR/mule/runtime}"

TICKET_OFFSET="${TICKET_OFFSET:-9587}"
TICKET_HTTP_PORT="${TICKET_HTTP_PORT:-17667}"
TICKET_HTTPS_PORT="${TICKET_HTTPS_PORT:-18030}"
TICKET_MANAGEMENT_PORT="${TICKET_MANAGEMENT_PORT:-19577}"
MULE_PORT="${MULE_PORT:-18083}"
BOOKING_PORT="${BOOKING_PORT:-18130}"
CLIENT_PORT="${CLIENT_PORT:-18230}"
PUBLIC_CLIENT_HOST="${PUBLIC_CLIENT_HOST:-helios.cs.ifmo.ru}"

TICKET_WAR="$PROJECT_DIR/ticket-service/target/ticket-service-1.0.0.war"
MULE_APP="$PROJECT_DIR/mule-rest-adapter/target/ticket-rest-adapter-1.0.0-mule-application.jar"
BOOKING_JAR="$PROJECT_DIR/booking-service/build/libs/booking-service-1.0.0.jar"
CLIENT_DIR="$PROJECT_DIR/client"
CLIENT_VENV="$CLIENT_DIR/.venv"
CLIENT_CERT_FILE="$PROJECT_DIR/wildfly/tls/client-cert.pem"
CLIENT_KEY_FILE="$PROJECT_DIR/wildfly/tls/client-key.pem"
SOAP_ENDPOINT="http://127.0.0.1:$TICKET_HTTP_PORT/ticket-service/ws"
SOAP_WSDL_URL="$SOAP_ENDPOINT/tickets.wsdl"
MULE_API_URL="http://127.0.0.1:$MULE_PORT/api/v1"
BOOKING_API_URL="http://127.0.0.1:$BOOKING_PORT/booking"

JAVA_LIMITS=(
    -Xms32m
    -Xmx128m
    -XX:MaxMetaspaceSize=128m
    -XX:CompressedClassSpaceSize=64m
    -Xss256k
    -XX:ActiveProcessorCount=1
    -XX:+UseSerialGC
    --add-opens=java.base/java.lang=ALL-UNNAMED
)

mkdir -p "$RUN_DIR"

cleanup_on_error() {
    "$SCRIPT_DIR/stop-lab4-helios.sh" >/dev/null 2>&1 || true
}
trap cleanup_on_error ERR

port_is_busy() {
    (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1
}

require_files() {
    local missing=0
    local path
    for path in "$TICKET_WAR" "$MULE_APP" "$BOOKING_JAR"; do
        if [[ ! -r "$path" ]]; then
            echo "Не найден артефакт: $path" >&2
            missing=1
        fi
    done
    for path in "$WILDFLY_HOME/bin/standalone.sh" "$MULE_HOME/bin/mule"; do
        if [[ ! -x "$path" ]]; then
            echo "Не найден исполняемый файл: $path" >&2
            missing=1
        fi
    done
    if ((missing)); then
        echo "Установите runtimes и соберите лабораторную перед запуском." >&2
        exit 1
    fi
}

require_commands() {
    local command_name
    for command_name in java curl openssl; do
        if ! command -v "$command_name" >/dev/null 2>&1; then
            echo "Не найдена команда '$command_name'." >&2
            exit 1
        fi
    done

    PYTHON_BIN="${PYTHON_BIN:-}"
    if [[ -z "$PYTHON_BIN" ]]; then
        for command_name in python3 python3.13 python3.12 python3.11 python3.10; do
            if command -v "$command_name" >/dev/null 2>&1; then
                PYTHON_BIN="$(command -v "$command_name")"
                break
            fi
        done
    fi
    if [[ -z "$PYTHON_BIN" || ! -x "$PYTHON_BIN" ]]; then
        echo "Не найден Python 3." >&2
        exit 1
    fi
}

check_ports() {
    local busy=()
    local port
    for port in "$TICKET_HTTP_PORT" "$TICKET_HTTPS_PORT" "$TICKET_MANAGEMENT_PORT" \
            "$MULE_PORT" "$BOOKING_PORT" "$CLIENT_PORT"; do
        if port_is_busy "$port"; then
            busy+=("$port")
        fi
    done
    if ((${#busy[@]})); then
        echo "Порты уже заняты: ${busy[*]}" >&2
        echo "Выполните ./scripts/stop-lab4-helios.sh и повторите запуск." >&2
        exit 1
    fi
}

wait_for_url() {
    local name="$1"
    local url="$2"
    local pid="$3"
    local log_file="$4"
    local attempts="${5:-120}"

    for ((attempt = 1; attempt <= attempts; attempt++)); do
        if curl -fsS --max-time 3 "$url" >/dev/null 2>&1; then
            echo "$name запущен (PID $pid)."
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name завершился с ошибкой." >&2
            tail -80 "$log_file" >&2 || true
            exit 1
        fi
        sleep 1
    done

    echo "$name не успел запуститься: $url" >&2
    tail -80 "$log_file" >&2 || true
    exit 1
}

start_ticket() {
    local log_file="$RUN_DIR/ticket-wildfly.log"
    local deployment="$WILDFLY_TICKET_BASE/deployments/ticket-service.war"

    mkdir -p "$WILDFLY_TICKET_BASE/deployments" "$WILDFLY_TICKET_BASE/log"
    rm -f "$deployment.deployed" "$deployment.failed" \
        "$deployment.isdeploying" "$deployment.undeployed"
    cp "$TICKET_WAR" "$deployment"
    touch "$deployment.dodeploy"
    : >"$log_file"

    export JAVA_OPTS="-Xms32m -Xmx160m -XX:MaxMetaspaceSize=256m -XX:CompressedClassSpaceSize=64m -Xss256k -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -Dorg.jboss.weld.bootstrap.concurrentDeployment=false --add-opens=java.base/java.util=ALL-UNNAMED"
    nohup "$WILDFLY_HOME/bin/standalone.sh" \
        -Djboss.server.base.dir="$WILDFLY_TICKET_BASE" \
        -Djboss.socket.binding.port-offset="$TICKET_OFFSET" \
        -Djboss.bind.address=127.0.0.1 \
        -Djboss.bind.address.management=127.0.0.1 \
        -Djboss.tx.node.id=ticket-lab4 \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$RUN_DIR/ticket.pid"
    wait_for_url "SOAP Ticket Service" "$SOAP_WSDL_URL" "$pid" "$log_file" 150
}

start_mule() {
    local log_file="$RUN_DIR/mule.log"
    mkdir -p "$MULE_HOME/apps"
    rm -f "$MULE_HOME/apps/ticket-rest-adapter.jar" \
        "$MULE_HOME/apps/ticket-rest-adapter-anchor.txt"
    cp "$MULE_APP" "$MULE_HOME/apps/ticket-rest-adapter.jar"
    : >"$log_file"

    if [[ "$(uname -s)" == "FreeBSD" ]]; then
        local mule_java="$(command -v java)"
        if [[ -x /usr/local/openjdk17/bin/java ]]; then
            mule_java=/usr/local/openjdk17/bin/java
        fi
        nohup env \
            MULE_HOME="$MULE_HOME" \
            JAVA_BIN="$mule_java" \
            HTTP_HOST=127.0.0.1 \
            HTTP_PORT="$MULE_PORT" \
            SOAP_WSDL_URL="$SOAP_WSDL_URL" \
            SOAP_ENDPOINT_URL="$SOAP_ENDPOINT" \
            "$SCRIPT_DIR/run-mule-kernel-direct.sh" \
            >"$log_file" 2>&1 &
    else
        nohup "$MULE_HOME/bin/mule" \
            -M-Xms64m \
            -M-Xmx384m \
            -M-XX:MaxMetaspaceSize=192m \
            -M-XX:ActiveProcessorCount=1 \
            -M-Dhttp.host=127.0.0.1 \
            -M-Dhttp.port="$MULE_PORT" \
            -M-Dticket.soap.wsdl="$SOAP_WSDL_URL" \
            -M-Dticket.soap.endpoint="$SOAP_ENDPOINT" \
            >"$log_file" 2>&1 &
    fi
    local pid=$!
    echo "$pid" >"$RUN_DIR/mule.pid"
    wait_for_url "Mule REST adapter" \
        "http://127.0.0.1:$MULE_PORT/health" "$pid" "$log_file" 180
}

start_booking() {
    local log_file="$RUN_DIR/booking.log"
    : >"$log_file"
    nohup env \
        TICKET_SERVICE_URL="$MULE_API_URL" \
        BOOKING_SERVICE_PORT="$BOOKING_PORT" \
        BOOKING_STORE_PATH="$RUN_DIR/booking.db" \
        java "${JAVA_LIMITS[@]}" -jar "$BOOKING_JAR" \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$RUN_DIR/booking.pid"
    wait_for_url "Booking Service" "$BOOKING_API_URL/actuator/health" \
        "$pid" "$log_file" 120
}

start_client() {
    local log_file="$RUN_DIR/client.log"
    if [[ ! -x "$CLIENT_VENV/bin/python" ]]; then
        "$PYTHON_BIN" -m venv "$CLIENT_VENV"
    fi
    if ! "$CLIENT_VENV/bin/python" -c 'import flask, requests' >/dev/null 2>&1; then
        "$CLIENT_VENV/bin/pip" install --quiet -r "$CLIENT_DIR/requirements.txt"
    fi

    PUBLIC_CLIENT_HOST="$PUBLIC_CLIENT_HOST" bash "$SCRIPT_DIR/setup-client-tls.sh"
    : >"$log_file"
    nohup env \
        TICKET_SERVICE_URL="$MULE_API_URL" \
        BOOKING_SERVICE_URL="$BOOKING_API_URL" \
        CLIENT_PORT="$CLIENT_PORT" \
        CLIENT_CERT_FILE="$CLIENT_CERT_FILE" \
        CLIENT_KEY_FILE="$CLIENT_KEY_FILE" \
        "$CLIENT_VENV/bin/python" "$CLIENT_DIR/app.py" \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$RUN_DIR/client.pid"

    for _ in {1..60}; do
        if curl -kfsS --max-time 3 "https://127.0.0.1:$CLIENT_PORT/" \
                >/dev/null 2>&1; then
            echo "HTTPS client запущен (PID $pid)."
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "HTTPS client завершился с ошибкой." >&2
            tail -80 "$log_file" >&2 || true
            exit 1
        fi
        sleep 1
    done
    echo "HTTPS client не успел запуститься." >&2
    exit 1
}

require_files
require_commands
check_ports
start_ticket
start_mule
start_booking
start_client

SOAP_WSDL_URL="$SOAP_WSDL_URL" \
MULE_API_URL="$MULE_API_URL" \
MULE_HEALTH_URL="http://127.0.0.1:$MULE_PORT/health" \
BOOKING_API_URL="$BOOKING_API_URL" \
    "$SCRIPT_DIR/verify-lab4-local.sh"

trap - ERR

echo
echo "SOAP WSDL:      $SOAP_WSDL_URL"
echo "Mule REST API:  $MULE_API_URL"
echo "Booking API:    $BOOKING_API_URL"
echo "Client:         https://$PUBLIC_CLIENT_HOST:$CLIENT_PORT"
