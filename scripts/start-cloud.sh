#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.run/cloud"
ENABLE_OBSERVABILITY="${ENABLE_OBSERVABILITY:-false}"

if [[ "$ENABLE_OBSERVABILITY" != "true" \
        && "$ENABLE_OBSERVABILITY" != "false" ]]; then
    echo "ENABLE_OBSERVABILITY должен быть true или false." >&2
    exit 1
fi

if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
    STOP_SCRIPT="$SCRIPT_DIR/stop-cloud-local.sh"
else
    STOP_SCRIPT="$SCRIPT_DIR/stop-cloud-helios.sh"
fi

CONFIG_PORT="${CONFIG_PORT:-18330}"
EUREKA_PORT="${EUREKA_PORT:-18430}"
TICKET_PORT="${TICKET_PORT:-18030}"
GATEWAY_PORT="${GATEWAY_PORT:-18530}"
BOOKING_LOAD_BALANCER_PORT="${BOOKING_LOAD_BALANCER_PORT:-18130}"
BOOKING_INSTANCE_1_PORT="${BOOKING_INSTANCE_1_PORT:-18131}"
BOOKING_INSTANCE_2_PORT="${BOOKING_INSTANCE_2_PORT:-18132}"
CLIENT_PORT="${CLIENT_PORT:-18230}"
JAEGER_UI_PORT="16686"
OTLP_HTTP_PORT="4318"
JAEGER_METRICS_PORT="8888"

CONFIG_JAR="$PROJECT_DIR/config-server/target/config-server-1.0.0.jar"
EUREKA_JAR="$PROJECT_DIR/eureka-server/target/eureka-server-1.0.0.jar"
TICKET_JAR="$PROJECT_DIR/ticket-service/target/ticket-service-1.0.0.war"
GATEWAY_JAR="$PROJECT_DIR/api-gateway/target/api-gateway-1.0.0.jar"
BOOKING_JAR="$PROJECT_DIR/booking-service/build/libs/booking-service-1.0.0.jar"
HAPROXY_CONFIG="$PROJECT_DIR/haproxy/haproxy.cfg"
CLIENT_DIR="$PROJECT_DIR/client"
CLIENT_VENV="$CLIENT_DIR/.venv"
CLIENT_CERT_FILE="$PROJECT_DIR/wildfly/tls/client-cert.pem"
CLIENT_KEY_FILE="$PROJECT_DIR/wildfly/tls/client-key.pem"
OBSERVABILITY_DIR="$PROJECT_DIR/tools/observability"
OTEL_JAVA_AGENT="$OBSERVABILITY_DIR/opentelemetry-javaagent.jar"
JAEGER_BINARY="$OBSERVABILITY_DIR/jaeger"

JAVA_OPTS=(
    -Xms32m
    -Xmx128m
    -XX:MaxMetaspaceSize=128m
    -Xss256k
    -XX:ActiveProcessorCount=1
    -XX:+UseSerialGC
    --add-opens=java.base/java.lang=ALL-UNNAMED
)

mkdir -p "$RUN_DIR"

cleanup_on_error() {
    "$STOP_SCRIPT" >/dev/null 2>&1 || true
}

trap cleanup_on_error ERR

port_is_busy() {
    local port="$1"
    (echo >/dev/tcp/127.0.0.1/"$port") >/dev/null 2>&1
}

require_artifacts() {
    local missing=0
    local artifacts=(
        "$CONFIG_JAR"
        "$EUREKA_JAR"
        "$TICKET_JAR"
        "$GATEWAY_JAR"
        "$BOOKING_JAR"
        "$HAPROXY_CONFIG"
    )
    if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
        artifacts+=("$OTEL_JAVA_AGENT" "$JAEGER_BINARY")
    fi
    local artifact
    for artifact in "${artifacts[@]}"; do
        if [[ ! -r "$artifact" ]]; then
            echo "Не найден $artifact" >&2
            missing=1
        fi
    done
    if ((missing)); then
        echo "Выполните ./scripts/build-cloud.sh." >&2
        if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
            echo "Для локальной трассировки также выполните ./scripts/setup-observability.sh." >&2
        fi
        exit 1
    fi
}

require_commands() {
    local command_name
    for command_name in java curl haproxy python3; do
        if ! command -v "$command_name" >/dev/null 2>&1; then
            echo "Не найдена команда '$command_name'." >&2
            [[ "$command_name" == "haproxy" ]] && \
                echo "Установите HAProxy перед запуском лабораторной №3." >&2
            exit 1
        fi
    done
}

check_ports() {
    local ports=(
        "$CONFIG_PORT"
        "$EUREKA_PORT"
        "$TICKET_PORT"
        "$GATEWAY_PORT"
        "$BOOKING_LOAD_BALANCER_PORT"
        "$BOOKING_INSTANCE_1_PORT"
        "$BOOKING_INSTANCE_2_PORT"
        "$CLIENT_PORT"
    )
    if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
        ports+=("$JAEGER_UI_PORT" "$OTLP_HTTP_PORT" "$JAEGER_METRICS_PORT")
    fi
    local busy=()
    local port
    for port in "${ports[@]}"; do
        if port_is_busy "$port"; then
            busy+=("$port")
        fi
    done
    if ((${#busy[@]})); then
        echo "Порты уже заняты: ${busy[*]}" >&2
        echo "Выполните ./scripts/stop-cloud-helios.sh и повторите запуск." >&2
        exit 1
    fi
}

start_haproxy() {
    local log_file="$RUN_DIR/haproxy.log"
    local pid_file="$RUN_DIR/haproxy.pid"
    : >"$log_file"

    export BOOKING_LOAD_BALANCER_PORT
    export BOOKING_INSTANCE_1_PORT
    export BOOKING_INSTANCE_2_PORT

    nohup haproxy -db -f "$HAPROXY_CONFIG" >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$pid_file"
    wait_for_health "HAProxy" \
        "http://127.0.0.1:$BOOKING_LOAD_BALANCER_PORT/booking/actuator/health" \
        "$pid" "$log_file"
}

wait_for_health() {
    local name="$1"
    local url="$2"
    local pid="$3"
    local log_file="$4"

    for _ in {1..120}; do
        if curl -fkSs --max-time 2 "$url" >/dev/null 2>&1; then
            echo "$name запущен (PID $pid)."
            return
        fi
        if ! kill -0 "$pid" 2>/dev/null; then
            echo "$name завершился с ошибкой." >&2
            tail -80 "$log_file" >&2
            exit 1
        fi
        sleep 1
    done

    echo "$name не успел запуститься." >&2
    tail -80 "$log_file" >&2
    exit 1
}

start_client() {
    local log_file="$RUN_DIR/client.log"
    local pid_file="$RUN_DIR/client.pid"

    if [[ ! -x "$CLIENT_VENV/bin/python" ]]; then
        echo "Создание виртуального окружения клиента..."
        python3 -m venv "$CLIENT_VENV"
    fi
    local requirements_file="$CLIENT_DIR/requirements.txt"
    local import_check='import flask, requests'
    local client_command=("$CLIENT_VENV/bin/python" "$CLIENT_DIR/app.py")
    local telemetry_environment=()

    if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
        requirements_file="$CLIENT_DIR/requirements-observability.txt"
        import_check='import flask, requests, opentelemetry.instrumentation.flask, opentelemetry.instrumentation.requests'
        client_command=(
            "$CLIENT_VENV/bin/opentelemetry-instrument"
            "$CLIENT_VENV/bin/python"
            "$CLIENT_DIR/app.py"
        )
        telemetry_environment=(
            "OTEL_SERVICE_NAME=flask-client"
            "OTEL_RESOURCE_ATTRIBUTES=service.instance.id=client-1,deployment.environment.name=lab3"
            "OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:$OTLP_HTTP_PORT"
            "OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf"
            "OTEL_TRACES_SAMPLER=always_on"
            "OTEL_BSP_SCHEDULE_DELAY=1000"
            "OTEL_METRICS_EXPORTER=none"
            "OTEL_LOGS_EXPORTER=none"
        )
    fi

    if ! "$CLIENT_VENV/bin/python" -c "$import_check" >/dev/null 2>&1; then
        echo "Установка зависимостей клиента..."
        "$CLIENT_VENV/bin/python" -m pip install --quiet \
            -r "$requirements_file"
    fi

    bash "$SCRIPT_DIR/setup-client-tls.sh"
    : >"$log_file"
    nohup env \
        "${telemetry_environment[@]}" \
        TICKET_SERVICE_URL="http://127.0.0.1:$GATEWAY_PORT/api/v1" \
        BOOKING_SERVICE_URL="http://127.0.0.1:$GATEWAY_PORT/booking" \
        CLIENT_PORT="$CLIENT_PORT" \
        CLIENT_CERT_FILE="$CLIENT_CERT_FILE" \
        CLIENT_KEY_FILE="$CLIENT_KEY_FILE" \
        "${client_command[@]}" \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$pid_file"
    wait_for_health "Client" "https://127.0.0.1:$CLIENT_PORT/" \
        "$pid" "$log_file"
}

start_service() {
    local id="$1"
    local name="$2"
    local jar="$3"
    local health_url="$4"
    local telemetry_service_name="$5"
    shift 5

    local log_file="$RUN_DIR/$id.log"
    local pid_file="$RUN_DIR/$id.pid"
    : >"$log_file"

    if [[ "$ENABLE_OBSERVABILITY" == "true" \
            && -n "$telemetry_service_name" ]]; then
        nohup env \
            OTEL_SERVICE_NAME="$telemetry_service_name" \
            OTEL_RESOURCE_ATTRIBUTES="service.instance.id=$id,deployment.environment.name=lab3" \
            OTEL_EXPORTER_OTLP_ENDPOINT="http://127.0.0.1:$OTLP_HTTP_PORT" \
            OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf" \
            OTEL_TRACES_SAMPLER="always_on" \
            OTEL_BSP_SCHEDULE_DELAY="1000" \
            OTEL_METRICS_EXPORTER="none" \
            OTEL_LOGS_EXPORTER="none" \
            java "${JAVA_OPTS[@]}" -javaagent:"$OTEL_JAVA_AGENT" -jar "$jar" "$@" \
            >"$log_file" 2>&1 &
    else
        nohup java "${JAVA_OPTS[@]}" -jar "$jar" "$@" \
            >"$log_file" 2>&1 &
    fi
    local pid=$!
    echo "$pid" >"$pid_file"
    wait_for_health "$name" "$health_url" "$pid" "$log_file"
}

start_jaeger() {
    local log_file="$RUN_DIR/jaeger.log"
    local pid_file="$RUN_DIR/jaeger.pid"
    : >"$log_file"

    nohup "$JAEGER_BINARY" >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" >"$pid_file"
    wait_for_health "Jaeger" "http://127.0.0.1:$JAEGER_UI_PORT/" \
        "$pid" "$log_file"
}

require_artifacts
require_commands
check_ports

export CONFIG_REPOSITORY="file:$PROJECT_DIR/config-repo"
export CONFIG_SERVER_URL="http://127.0.0.1:$CONFIG_PORT"
export EUREKA_SERVER_URL="http://127.0.0.1:$EUREKA_PORT/eureka/"
export BOOKING_LOAD_BALANCER_URL="http://127.0.0.1:$BOOKING_LOAD_BALANCER_PORT"
export BOOKING_STORE_PATH="$RUN_DIR/booking.db"

if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
    start_jaeger
fi

start_service config "Config Server" "$CONFIG_JAR" \
    "http://127.0.0.1:$CONFIG_PORT/actuator/health" \
    "" \
    --server.address=127.0.0.1 \
    --server.port="$CONFIG_PORT"

start_service eureka "Eureka Server" "$EUREKA_JAR" \
    "http://127.0.0.1:$EUREKA_PORT/actuator/health" \
    "" \
    --server.address=127.0.0.1 \
    --server.port="$EUREKA_PORT"

start_service ticket "Ticket Service" "$TICKET_JAR" \
    "http://127.0.0.1:$TICKET_PORT/api/v1/actuator/health" \
    "ticket-service" \
    --server.address=127.0.0.1 \
    --server.port="$TICKET_PORT"

start_service gateway "Zuul API Gateway" "$GATEWAY_JAR" \
    "http://127.0.0.1:$GATEWAY_PORT/actuator/health" \
    "api-gateway" \
    --server.address=127.0.0.1 \
    --server.port="$GATEWAY_PORT"

export TICKET_SERVICE_URL="http://127.0.0.1:$GATEWAY_PORT/api/v1"

start_service booking-1 "Booking Service #1" "$BOOKING_JAR" \
    "http://127.0.0.1:$BOOKING_INSTANCE_1_PORT/booking/actuator/health" \
    "booking-service" \
    --server.address=127.0.0.1 \
    --server.port="$BOOKING_INSTANCE_1_PORT"

start_service booking-2 "Booking Service #2" "$BOOKING_JAR" \
    "http://127.0.0.1:$BOOKING_INSTANCE_2_PORT/booking/actuator/health" \
    "booking-service" \
    --server.address=127.0.0.1 \
    --server.port="$BOOKING_INSTANCE_2_PORT"

start_haproxy
start_client

trap - ERR

echo
echo "Config Server: http://127.0.0.1:$CONFIG_PORT"
echo "Eureka Server: http://127.0.0.1:$EUREKA_PORT"
echo "Ticket direct: http://127.0.0.1:$TICKET_PORT/api/v1"
echo "Booking #1:   http://127.0.0.1:$BOOKING_INSTANCE_1_PORT/booking"
echo "Booking #2:   http://127.0.0.1:$BOOKING_INSTANCE_2_PORT/booking"
echo "HAProxy:      http://127.0.0.1:$BOOKING_LOAD_BALANCER_PORT/booking"
echo "Public API:   http://127.0.0.1:$GATEWAY_PORT"
echo "Client:       https://127.0.0.1:$CLIENT_PORT"
if [[ "$ENABLE_OBSERVABILITY" == "true" ]]; then
    echo "Jaeger UI:    http://127.0.0.1:$JAEGER_UI_PORT"
    echo "Демо trace:   ./scripts/demo-tracing.sh"
fi
