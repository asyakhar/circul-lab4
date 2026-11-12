#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
HELIOS_USER="${HELIOS_USER:-s409792}"
HELIOS_HOST="${HELIOS_HOST:-helios.cs.ifmo.ru}"
HELIOS_SSH_PORT="${HELIOS_SSH_PORT:-2222}"
PUBLIC_PATH="${PUBLIC_PATH:-circul}"
HELIOS_KEY="${HELIOS_KEY:-}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/circul-se.XXXXXX")"
ARCHIVE="$TMP_DIR/circul-public.tgz"
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

tar -czf "$ARCHIVE" \
    -C "$PROJECT_DIR/client" \
    index-public.html \
    proxy.php \
    static

echo "Подключение к $HELIOS_USER@$HELIOS_HOST..."
ssh "${SSH_OPTIONS[@]}" -MNf "$HELIOS_USER@$HELIOS_HOST"

scp "${SCP_OPTIONS[@]}" "$ARCHIVE" \
    "$HELIOS_USER@$HELIOS_HOST:circul-public.tgz"

ssh "${SSH_OPTIONS[@]}" "$HELIOS_USER@$HELIOS_HOST" \
    "set -e; \
     tunnel_pid=''; \
     if [ -r \"\$HOME/circul-cloud/.run/cloud/public-tunnel.pid\" ]; then \
         IFS= read -r tunnel_pid < \"\$HOME/circul-cloud/.run/cloud/public-tunnel.pid\" || true; \
     fi; \
     case \"\$tunnel_pid\" in (*[!0-9]*|'') ;; (*) kill -9 \"\$tunnel_pid\" 2>/dev/null || true ;; esac; \
     client_pid=''; \
     if [ -r \"\$HOME/circul-cloud/.run/cloud/client.pid\" ]; then \
         IFS= read -r client_pid < \"\$HOME/circul-cloud/.run/cloud/client.pid\" || true; \
     fi; \
     case \"\$client_pid\" in (*[!0-9]*|'') ;; (*) kill -9 \"\$client_pid\" 2>/dev/null || true ;; esac; \
     public_dir=\"\$HOME/public_html/$PUBLIC_PATH\"; \
     mkdir -p \"\$public_dir\"; \
     tar -xzf \"\$HOME/circul-public.tgz\" -C \"\$public_dir\"; \
     mv \"\$public_dir/index-public.html\" \"\$public_dir/index.html\"; \
     rm -f \"\$HOME/circul-public.tgz\"; \
     chmod 755 \"\$HOME/public_html\" \"\$public_dir\"; \
     find \"\$public_dir\" -type d -exec chmod 755 {} +; \
     find \"\$public_dir\" -type f -exec chmod 644 {} +"

public_url="https://se.ifmo.ru/~$HELIOS_USER/$PUBLIC_PATH/"
api_test="${public_url}proxy.php?service=ticket&path=/tickets"

echo "Проверка страницы..."
curl -fsS --max-time 20 "$public_url" >/dev/null
echo "Проверка API через se.ifmo.ru..."
api_response="$TMP_DIR/api-response.txt"
api_status="$(curl -sS --max-time 20 -o "$api_response" -w '%{http_code}' "$api_test")"
if [[ ! "$api_status" =~ ^2[0-9][0-9]$ ]]; then
    echo "API вернул HTTP $api_status:"
    cat "$api_response"
    echo
    exit 1
fi
echo "Готово: $public_url"
