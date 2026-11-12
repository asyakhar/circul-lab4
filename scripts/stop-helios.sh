#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_HOME="$LAB2_DIR/wildfly/wildfly-ee-10-41.0.1.Final"
export JAVA_OPTS="-Xms16m -Xmx96m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC"

stop_pid_file() {
    local pid_file="$1"

    if [[ -f "$pid_file" ]]; then
        local pid
        pid="$(cat "$pid_file")"
        if [[ "$pid" =~ ^[0-9]+$ ]]; then
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$pid_file"
    fi
}

stop_wildfly_processes() {
    local base="$1"
    local pid

    # PID-файл мог быть перезаписан неудачным повторным запуском. Поэтому
    # дополнительно находим только процессы текущего пользователя и сервера.
    while read -r pid; do
        [[ -n "$pid" ]] && kill "$pid" 2>/dev/null || true
    done < <(pgrep -u "$(id -u)" -f -- "-Djboss.server.base.dir=$base" || true)

    stop_pid_file "$base/server.pid"
}

stop_pid_file "$LAB2_DIR/client/client.pid"

"$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
    --controller=127.0.0.1:19677 --command=':shutdown' >/dev/null 2>&1 || true
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
    --controller=127.0.0.1:19577 --command=':shutdown' >/dev/null 2>&1 || true

# CLI обычно завершает сервер корректно. Эти вызовы также убирают зависшие
# экземпляры, если management-интерфейс уже недоступен.
stop_wildfly_processes "$LAB2_DIR/wildfly/servers/booking"
stop_wildfly_processes "$LAB2_DIR/wildfly/servers/ticket"

for _ in {1..20}; do
    if ! pgrep -u "$(id -u)" -f -- "-Djboss.server.base.dir=$LAB2_DIR/wildfly/servers/" >/dev/null; then
        break
    fi
    sleep 0.25
done

echo "Сервисы Helios остановлены."
