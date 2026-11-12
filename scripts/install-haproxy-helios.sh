#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
HAPROXY_VERSION="3.2.26"
SOURCE_ARCHIVE="$PROJECT_DIR/haproxy-$HAPROXY_VERSION.tar.gz"
INSTALL_DIR="${HAPROXY_INSTALL_DIR:-$HOME/.local/bin}"
BUILD_DIR="$PROJECT_DIR/.run/haproxy-build"

export PATH="$HOME/.local/bin:/usr/local/bin:/usr/local/sbin:/usr/bin:/bin:$PATH"

if command -v haproxy >/dev/null 2>&1; then
    echo "HAProxy уже установлен: $(command -v haproxy)"
    exit 0
fi

if [[ ! -r "$SOURCE_ARCHIVE" ]]; then
    echo "Не найден исходный архив $SOURCE_ARCHIVE" >&2
    exit 1
fi

if command -v gmake >/dev/null 2>&1; then
    MAKE_BIN="$(command -v gmake)"
else
    echo "Для сборки HAProxy на Helios нужна команда gmake." >&2
    exit 1
fi

if ! command -v cc >/dev/null 2>&1; then
    echo "Для сборки HAProxy на Helios нужен C-компилятор cc." >&2
    exit 1
fi

echo "Сборка HAProxy $HAPROXY_VERSION в пользовательском каталоге..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR" "$INSTALL_DIR"
tar -xzf "$SOURCE_ARCHIVE" -C "$BUILD_DIR"

"$MAKE_BIN" -C "$BUILD_DIR/haproxy-$HAPROXY_VERSION" \
    -j2 TARGET=freebsd

cp "$BUILD_DIR/haproxy-$HAPROXY_VERSION/haproxy" "$INSTALL_DIR/haproxy"
chmod 755 "$INSTALL_DIR/haproxy"
"$INSTALL_DIR/haproxy" -v

