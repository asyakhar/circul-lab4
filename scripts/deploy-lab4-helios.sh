#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

HELIOS_USER="${HELIOS_USER:-s408303}"
HELIOS_HOST="${HELIOS_HOST:-helios.cs.ifmo.ru}"
HELIOS_SSH_PORT="${HELIOS_SSH_PORT:-2222}"
HELIOS_REMOTE_DIR="${HELIOS_REMOTE_DIR:-/tmp/$HELIOS_USER/circul-lab4}"
HELIOS_KEY="${HELIOS_KEY:-}"
MULE_ARCHIVE="${MULE_ARCHIVE:-}"
MULE_LICENSE="${MULE_LICENSE:-}"
CLIENT_PORT="${CLIENT_PORT:-18230}"
PUBLIC_CLIENT_HOST="${PUBLIC_CLIENT_HOST:-$HELIOS_HOST}"
PUBLIC_PATH="${PUBLIC_PATH:-circul}"

if [[ -z "$HELIOS_KEY" && -r "$HOME/.ssh/helios/main" ]]; then
    HELIOS_KEY="$HOME/.ssh/helios/main"
fi

case "$HELIOS_REMOTE_DIR" in
    (*[!A-Za-z0-9._/-]*|'')
        echo "HELIOS_REMOTE_DIR содержит недопустимые символы: $HELIOS_REMOTE_DIR" >&2
        exit 1
        ;;
esac

if [[ "$HELIOS_REMOTE_DIR" == /* \
        && "$HELIOS_REMOTE_DIR" != "/tmp/$HELIOS_USER/"* ]]; then
    echo "Абсолютный HELIOS_REMOTE_DIR разрешён только внутри /tmp/$HELIOS_USER/." >&2
    exit 1
fi

if [[ -z "$MULE_ARCHIVE" ]]; then
    MULE_ARCHIVE="$($SCRIPT_DIR/download-mule-kernel.sh)"
elif [[ ! -r "$MULE_ARCHIVE" ]]; then
    echo "Не найден Mule standalone ZIP: $MULE_ARCHIVE" >&2
    exit 1
fi

if [[ -n "$MULE_LICENSE" && ! -r "$MULE_LICENSE" ]]; then
    echo "Не найден файл лицензии Mule: $MULE_LICENSE" >&2
    exit 1
fi

if ! unzip -Z1 "$MULE_ARCHIVE" \
        | awk '$0 ~ /(^|\/)bin\/mule$/ { found = 1 } END { exit !found }'; then
    echo "Архив не похож на Mule standalone distribution: $MULE_ARCHIVE" >&2
    exit 1
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/circul-lab4-helios.XXXXXX")"
APP_ARCHIVE="$TMP_DIR/circul-lab4.tgz"
CONTROL_SOCKET="$TMP_DIR/ssh-control"

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
    "$SCRIPT_DIR/build-lab4.sh"
fi

required_artifacts=(
    ticket-service/target/ticket-service-1.0.0.war
    booking-service/build/libs/booking-service-1.0.0.jar
    mule-rest-adapter/target/ticket-rest-adapter-1.0.0-mule-application.jar
)
for artifact in "${required_artifacts[@]}"; do
    if [[ ! -r "$PROJECT_DIR/$artifact" ]]; then
        echo "Не найден артефакт $artifact. Выполните ./scripts/build-lab4.sh." >&2
        exit 1
    fi
done

echo "Упаковка лабораторной №4..."
tar -czf "$APP_ARCHIVE" \
    -C "$PROJECT_DIR" \
    --exclude='client/.venv' \
    --exclude='client/__pycache__' \
    --exclude='client/*.log' \
    --exclude='client/tls' \
    LAB4.md \
    client \
    scripts/install-wildfly-helios.sh \
    scripts/setup-ticket-wildfly-lab4-helios.sh \
    scripts/install-mule-helios.sh \
    scripts/run-mule-kernel-direct.sh \
    scripts/start-lab4-helios.sh \
    scripts/stop-lab4-helios.sh \
    scripts/setup-client-tls.sh \
    scripts/verify-lab4-local.sh \
    "${required_artifacts[@]}"

echo "Подключение к $HELIOS_USER@$HELIOS_HOST:$HELIOS_SSH_PORT..."
echo "Без SSH-ключа пароль потребуется ввести один раз."
ssh "${SSH_OPTIONS[@]}" -MNf "$HELIOS_USER@$HELIOS_HOST"

if [[ "$HELIOS_REMOTE_DIR" == /* ]]; then
    REMOTE_PATH="$HELIOS_REMOTE_DIR"
else
    REMOTE_PATH="\$HOME/$HELIOS_REMOTE_DIR"
fi
REMOTE_APP_ARCHIVE="$REMOTE_PATH/.circul-lab4-deploy.tgz"

echo "Проверка окружения Helios..."
ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "PATH=\"\$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin\"; \
     unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS; \
     missing=''; \
     for cmd in bash java curl unzip openssl; do \
         command -v \"\$cmd\" >/dev/null 2>&1 || missing=\"\$missing \$cmd\"; \
     done; \
     if [ -n \"\$missing\" ]; then \
         echo \"На Helios не найдены команды:\$missing\" >&2; exit 1; \
     fi; \
     java -Xms16m -Xmx32m -XX:MaxMetaspaceSize=64m -version"

echo "Загрузка приложения и Mule Runtime..."
ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "mkdir -p $REMOTE_PATH"
scp "${SCP_OPTIONS[@]}" "$APP_ARCHIVE" \
    "$HELIOS_USER@$HELIOS_HOST:$HELIOS_REMOTE_DIR/.circul-lab4-deploy.tgz"
scp "${SCP_OPTIONS[@]}" "$MULE_ARCHIVE" \
    "$HELIOS_USER@$HELIOS_HOST:$HELIOS_REMOTE_DIR/mule-standalone.zip"
if [[ -n "$MULE_LICENSE" ]]; then
    scp "${SCP_OPTIONS[@]}" "$MULE_LICENSE" \
        "$HELIOS_USER@$HELIOS_HOST:$HELIOS_REMOTE_DIR/mule-license.lic"
fi

echo "Установка и запуск..."
ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "set -e; \
     export PATH=\"\$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin\"; \
     unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS; \
     if [ -x \"$REMOTE_PATH/scripts/stop-lab4-helios.sh\" ]; then \
         \"$REMOTE_PATH/scripts/stop-lab4-helios.sh\" || true; \
     fi; \
     tar -xzf \"$REMOTE_APP_ARCHIVE\" -C \"$REMOTE_PATH\"; \
     rm -f \"$REMOTE_APP_ARCHIVE\"; \
     cd \"$REMOTE_PATH\"; \
     ./scripts/install-wildfly-helios.sh; \
     ./scripts/setup-ticket-wildfly-lab4-helios.sh; \
     if [ -r ./mule-license.lic ]; then \
         MULE_LICENSE=./mule-license.lic ./scripts/install-mule-helios.sh; \
         rm -f ./mule-license.lic; \
     else \
         ./scripts/install-mule-helios.sh; \
     fi; \
     rm -f ./mule-standalone.zip; \
     PUBLIC_CLIENT_HOST='$PUBLIC_CLIENT_HOST' CLIENT_PORT='$CLIENT_PORT' \
         ./scripts/start-lab4-helios.sh"

echo
echo "Публикация сайта через se.ifmo.ru..."
HELIOS_USER="$HELIOS_USER" \
HELIOS_HOST="$HELIOS_HOST" \
HELIOS_SSH_PORT="$HELIOS_SSH_PORT" \
HELIOS_KEY="$HELIOS_KEY" \
PUBLIC_PATH="$PUBLIC_PATH" \
    "$SCRIPT_DIR/publish-se-helios.sh"
