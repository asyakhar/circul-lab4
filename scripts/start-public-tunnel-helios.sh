#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_DIR="$PROJECT_DIR/.run/cloud"
CLIENT_PORT="${CLIENT_PORT:-18230}"
LOCAL_BIN="$HOME/.local/bin"
CLOUDFLARED_BIN="$LOCAL_BIN/cloudflared"
PID_FILE="$RUN_DIR/public-tunnel.pid"
LOG_FILE="$RUN_DIR/public-tunnel.log"
URL_FILE="$RUN_DIR/public-url.txt"

mkdir -p "$RUN_DIR" "$LOCAL_BIN"

if ! curl -kfsS --max-time 5 "https://127.0.0.1:$CLIENT_PORT/" >/dev/null; then
    echo "Клиент не отвечает на https://127.0.0.1:$CLIENT_PORT" >&2
    echo "Сначала запустите ./scripts/start-cloud-helios.sh" >&2
    exit 1
fi

if [[ -f "$PID_FILE" ]]; then
    old_pid="$(cat "$PID_FILE")"
    if [[ "$old_pid" =~ ^[0-9]+$ ]] && kill -0 "$old_pid" 2>/dev/null; then
        if [[ -s "$URL_FILE" ]]; then
            old_url="$(cat "$URL_FILE")"
            if curl -fsS --max-time 15 "$old_url/" >/dev/null 2>&1; then
                echo "Публичный туннель уже работает."
                echo "Public URL: $old_url"
                exit 0
            fi
            echo "Предыдущий туннель не отвечает; выполняется перезапуск..."
        fi
        kill "$old_pid" || true
        for _ in {1..20}; do
            kill -0 "$old_pid" 2>/dev/null || break
            sleep 0.25
        done
    fi
    rm -f "$PID_FILE" "$URL_FILE"
fi

if [[ ! -x "$CLOUDFLARED_BIN" ]]; then
    package_dir="$PROJECT_DIR/.run/cloudflared-package"
    extract_dir="$PROJECT_DIR/.run/cloudflared-extract"
    rm -rf "$package_dir" "$extract_dir"
    mkdir -p "$package_dir" "$extract_dir"

    freebsd_major="$(freebsd-version -u | cut -d. -f1)"
    case "$freebsd_major" in
        14|15) ;;
        *)
            echo "Неподдерживаемая версия FreeBSD: $freebsd_major" >&2
            exit 1
            ;;
    esac

    repository_url="https://pkg.freebsd.org/FreeBSD:${freebsd_major}:amd64/latest"
    echo "Получение индекса официальных пакетов FreeBSD $freebsd_major..."
    curl -fsSL "$repository_url/packagesite.pkg" \
        -o "$package_dir/packagesite.pkg"
    tar -xf "$package_dir/packagesite.pkg" -C "$package_dir"

    package_path="$(grep -m1 '"name":"cloudflared"' \
        "$package_dir/packagesite.yaml" \
        | sed -E 's/.*"path":"([^"]+)".*/\1/')"
    if [[ -z "$package_path" || "$package_path" == *'"'* ]]; then
        echo "В официальном репозитории FreeBSD не найден cloudflared." >&2
        exit 1
    fi

    echo "Загрузка FreeBSD-пакета cloudflared..."
    package_file="$package_dir/cloudflared.pkg"
    curl -fsSL "$repository_url/$package_path" -o "$package_file"

    tar -xf "$package_file" -C "$extract_dir"
    extracted_binary="$(find "$extract_dir" -type f -path '*/bin/cloudflared' | head -1)"
    if [[ -z "$extracted_binary" ]]; then
        echo "В пакете не найден бинарный файл cloudflared." >&2
        exit 1
    fi
    cp "$extracted_binary" "$CLOUDFLARED_BIN"
    chmod 755 "$CLOUDFLARED_BIN"
fi

: >"$LOG_FILE"
nohup "$CLOUDFLARED_BIN" tunnel \
    --no-autoupdate \
    --protocol http2 \
    --url "https://127.0.0.1:$CLIENT_PORT" \
    --no-tls-verify \
    >"$LOG_FILE" 2>&1 &
tunnel_pid=$!
echo "$tunnel_pid" >"$PID_FILE"

public_url=""
for _ in {1..120}; do
    public_url="$(grep -Eo 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG_FILE" | head -1 || true)"
    if [[ -n "$public_url" ]]; then
        echo "$public_url" >"$URL_FILE"
        if curl -fsS --max-time 15 "$public_url/" >/dev/null 2>&1; then
            echo "Публичный туннель запущен (PID $tunnel_pid)."
            echo "Public URL: $public_url"
            exit 0
        fi
    fi
    if ! kill -0 "$tunnel_pid" 2>/dev/null; then
        echo "cloudflared завершился с ошибкой." >&2
        tail -80 "$LOG_FILE" >&2
        exit 1
    fi
    sleep 1
done

echo "Туннель не стал доступен за 120 секунд." >&2
tail -80 "$LOG_FILE" >&2
exit 1

