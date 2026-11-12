#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HELIOS_USER="${HELIOS_USER:-s409792}"
HELIOS_HOST="${HELIOS_HOST:-helios.cs.ifmo.ru}"
HELIOS_SSH_PORT="${HELIOS_SSH_PORT:-2222}"
HELIOS_REMOTE_DIR="${HELIOS_REMOTE_DIR:-circul-cloud}"
HELIOS_KEY="${HELIOS_KEY:-}"
PUBLIC_PATH="${PUBLIC_PATH:-circul}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/circul-publish.XXXXXX")"
CONTROL_SOCKET="$TMP_DIR/ssh-control"
SSH_OPTIONS=(-p "$HELIOS_SSH_PORT" -o ControlMaster=auto -o ControlPersist=60 -o "ControlPath=$CONTROL_SOCKET")
SCP_OPTIONS=(-P "$HELIOS_SSH_PORT" -o ControlMaster=auto -o ControlPersist=60 -o "ControlPath=$CONTROL_SOCKET")

if [[ -n "$HELIOS_KEY" ]]; then
    SSH_OPTIONS+=(-i "$HELIOS_KEY")
    SCP_OPTIONS+=(-i "$HELIOS_KEY")
fi

cleanup() {
    ssh "${SSH_OPTIONS[@]}" -O exit "$HELIOS_USER@$HELIOS_HOST" >/dev/null 2>&1 || true
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

echo "Подключение к $HELIOS_USER@$HELIOS_HOST..."
ssh "${SSH_OPTIONS[@]}" -MNf "$HELIOS_USER@$HELIOS_HOST"

scp "${SCP_OPTIONS[@]}" \
    "$SCRIPT_DIR/start-public-tunnel-helios.sh" \
    "$SCRIPT_DIR/publish-personal-page-helios.sh" \
    "$SCRIPT_DIR/stop-cloud-helios.sh" \
    "$HELIOS_USER@$HELIOS_HOST:$HELIOS_REMOTE_DIR/scripts/"

output="$(ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "chmod +x \"\$HOME/$HELIOS_REMOTE_DIR/scripts/start-public-tunnel-helios.sh\"; \
     cd \"\$HOME/$HELIOS_REMOTE_DIR\"; \
     ./scripts/start-public-tunnel-helios.sh")"
echo "$output"

public_url="$(printf '%s\n' "$output" | grep -Eo 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1)"
if [[ -z "$public_url" ]]; then
    echo "Не удалось определить публичную ссылку." >&2
    exit 1
fi

ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "chmod +x \"\$HOME/$HELIOS_REMOTE_DIR/scripts/publish-personal-page-helios.sh\"; \
     PUBLIC_PATH='$PUBLIC_PATH' \
     \"\$HOME/$HELIOS_REMOTE_DIR/scripts/publish-personal-page-helios.sh\" '$public_url'"

echo "Проверка $public_url ..."
if curl -fsS --max-time 20 "$public_url/" >/dev/null; then
    personal_url="https://se.ifmo.ru/~$HELIOS_USER/$PUBLIC_PATH/"
    echo "Проверка персональной ссылки $personal_url ..."
    if curl -fsS --max-time 20 "$personal_url" | grep -Fq "$public_url"; then
        echo "Готово: $personal_url"
    else
        echo "Туннель работает, но персональная страница пока не обновилась:" >&2
        echo "$personal_url" >&2
        exit 3
    fi
else
    echo "Туннель создан, но ссылка пока не отвечает. Повторите проверку через минуту:" >&2
    echo "$public_url" >&2
    exit 2
fi

