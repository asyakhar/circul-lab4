#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

HELIOS_USER="${HELIOS_USER:-s409792}"
HELIOS_HOST="${HELIOS_HOST:-helios.cs.ifmo.ru}"
HELIOS_SSH_PORT="${HELIOS_SSH_PORT:-2222}"
HELIOS_REMOTE_DIR="${HELIOS_REMOTE_DIR:-circul-cloud}"
HELIOS_KEY="${HELIOS_KEY:-}"
CLIENT_PORT="${CLIENT_PORT:-18230}"
PUBLIC_CLIENT_HOST="${PUBLIC_CLIENT_HOST:-$HELIOS_HOST}"
HAPROXY_VERSION="3.2.26"
HAPROXY_SHA256="760ef35fafc1e05844e73ad07e138e9e96fddbf5de02f606249c027f754827da"
HAPROXY_URL="https://www.haproxy.org/download/3.2/src/haproxy-$HAPROXY_VERSION.tar.gz"

case "$HELIOS_REMOTE_DIR" in
    (*[!A-Za-z0-9._/-]*|'')
        echo "HELIOS_REMOTE_DIR содержит недопустимые символы: $HELIOS_REMOTE_DIR" >&2
        exit 1
        ;;
esac

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/circul-helios.XXXXXX")"
ARCHIVE="$TMP_DIR/circul-cloud.tgz"
CONTROL_SOCKET="$TMP_DIR/ssh-control"
DOWNLOAD_CACHE="$PROJECT_DIR/.run/deploy-cache"
HAPROXY_SOURCE="$DOWNLOAD_CACHE/haproxy-$HAPROXY_VERSION.tar.gz"

SSH_OPTIONS=(
    -p "$HELIOS_SSH_PORT"
    -o ControlMaster=auto
    -o ControlPersist=60
    -o "ControlPath=$CONTROL_SOCKET"
    -o ServerAliveInterval=30
    -o ServerAliveCountMax=3
)
SCP_OPTIONS=(
    -P "$HELIOS_SSH_PORT"
    -o ControlMaster=auto
    -o ControlPersist=60
    -o "ControlPath=$CONTROL_SOCKET"
)

if [[ -n "$HELIOS_KEY" ]]; then
    SSH_OPTIONS+=(-i "$HELIOS_KEY")
    SCP_OPTIONS+=(-i "$HELIOS_KEY")
fi

cleanup() {
    ssh "${SSH_OPTIONS[@]}" -O exit "$HELIOS_USER@$HELIOS_HOST" \
        >/dev/null 2>&1 || true
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
    "$SCRIPT_DIR/build-cloud.sh"
fi

required_artifacts=(
    config-server/target/config-server-1.0.0.jar
    eureka-server/target/eureka-server-1.0.0.jar
    ticket-service/target/ticket-service-1.0.0.war
    api-gateway/target/api-gateway-1.0.0.jar
    booking-service/build/libs/booking-service-1.0.0.jar
)

for artifact in "${required_artifacts[@]}"; do
    if [[ ! -r "$PROJECT_DIR/$artifact" ]]; then
        echo "Не найден артефакт $artifact. Выполните ./scripts/build-cloud.sh." >&2
        exit 1
    fi
done

mkdir -p "$DOWNLOAD_CACHE"
if [[ ! -r "$HAPROXY_SOURCE" ]]; then
    echo "Загрузка исходников HAProxy $HAPROXY_VERSION..."
    curl -fL "$HAPROXY_URL" -o "$HAPROXY_SOURCE"
fi

actual_haproxy_sha256="$(shasum -a 256 "$HAPROXY_SOURCE" | awk '{print $1}')"
if [[ "$actual_haproxy_sha256" != "$HAPROXY_SHA256" ]]; then
    echo "Неверная контрольная сумма исходников HAProxy." >&2
    rm -f "$HAPROXY_SOURCE"
    exit 1
fi

echo "Упаковка приложения..."
tar -czf "$ARCHIVE" \
    -C "$PROJECT_DIR" \
    --exclude='client/.venv' \
    --exclude='client/__pycache__' \
    --exclude='client/*.log' \
    README.md \
    config-repo \
    haproxy \
    client \
    scripts/start-cloud-helios.sh \
    scripts/stop-cloud-helios.sh \
    scripts/setup-client-tls.sh \
    scripts/install-haproxy-helios.sh \
    "${required_artifacts[@]}" \
    -C "$DOWNLOAD_CACHE" "haproxy-$HAPROXY_VERSION.tar.gz"

echo "Подключение к $HELIOS_USER@$HELIOS_HOST:$HELIOS_SSH_PORT..."
echo "Если SSH-ключ не настроен, пароль от Helios потребуется ввести один раз."
ssh "${SSH_OPTIONS[@]}" -MNf "$HELIOS_USER@$HELIOS_HOST"

REMOTE_PATH="\$HOME/$HELIOS_REMOTE_DIR"
REMOTE_ARCHIVE="\$HOME/circul-cloud-deploy.tgz"

echo "Проверка окружения Helios..."
ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "PATH=\"\$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin\"; \
     missing=''; \
     for cmd in bash java openssl; do \
         command -v \"\$cmd\" >/dev/null 2>&1 || missing=\"\$missing \$cmd\"; \
     done; \
     python_bin=''; \
     for cmd in python3 python3.13 python3.12 python3.11 python3.10; do \
         if command -v \"\$cmd\" >/dev/null 2>&1; then python_bin=\"\$(command -v \"\$cmd\")\"; break; fi; \
     done; \
     [ -n \"\$python_bin\" ] || missing=\"\$missing python3\"; \
     if ! command -v haproxy >/dev/null 2>&1; then \
         command -v gmake >/dev/null 2>&1 || missing=\"\$missing gmake\"; \
         command -v cc >/dev/null 2>&1 || missing=\"\$missing cc\"; \
     fi; \
     if [ -n \"\$missing\" ]; then \
         echo \"На Helios не найдены команды:\$missing\" >&2; exit 1; \
     fi; \
     echo \"Python: \$python_bin\"; \
     java -Xms16m -Xmx32m -XX:MaxMetaspaceSize=64m -version"

echo "Загрузка архива..."
scp "${SCP_OPTIONS[@]}" "$ARCHIVE" \
    "$HELIOS_USER@$HELIOS_HOST:circul-cloud-deploy.tgz"

echo "Установка и запуск..."
ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "set -e; \
     export PATH=\"\$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin\"; \
     if [ -x \"$REMOTE_PATH/scripts/stop-cloud-helios.sh\" ]; then \
         \"$REMOTE_PATH/scripts/stop-cloud-helios.sh\" || true; \
     fi; \
     mkdir -p \"$REMOTE_PATH\"; \
     tar -xzf \"$REMOTE_ARCHIVE\" -C \"$REMOTE_PATH\"; \
     rm -f \"$REMOTE_ARCHIVE\"; \
     cd \"$REMOTE_PATH\"; \
     ./scripts/install-haproxy-helios.sh; \
     python_bin=''; \
     for cmd in python3 python3.13 python3.12 python3.11 python3.10; do \
         if command -v \"\$cmd\" >/dev/null 2>&1; then python_bin=\"\$(command -v \"\$cmd\")\"; break; fi; \
     done; \
     PUBLIC_CLIENT_HOST='$PUBLIC_CLIENT_HOST' CLIENT_PORT='$CLIENT_PORT' PYTHON_BIN=\"\$python_bin\" \
         ./scripts/start-cloud-helios.sh"

PUBLIC_URL="https://$PUBLIC_CLIENT_HOST:$CLIENT_PORT"
echo
echo "Проверка внешнего адреса $PUBLIC_URL ..."
if curl -kfsS --max-time 15 "$PUBLIC_URL/" >/dev/null; then
    echo "Готово: $PUBLIC_URL"
else
    echo "Сервисы запущены, но порт недоступен снаружи." >&2
    echo "Проверьте через ./scripts/tunnel-helios.sh; для постоянной публичной" >&2
    echo "ссылки потребуется разрешённый кафедрой внешний порт или reverse proxy." >&2
    exit 2
fi
