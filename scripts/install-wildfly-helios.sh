#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB2_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WILDFLY_VERSION="41.0.1.Final"
WILDFLY_NAME="wildfly-ee-10-${WILDFLY_VERSION}"
WILDFLY_DIR="$LAB2_DIR/wildfly/$WILDFLY_NAME"
DOWNLOAD_URL="https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/${WILDFLY_NAME}.tar.gz"

if [[ -x "$WILDFLY_DIR/bin/standalone.sh" ]]; then
    echo "WildFly уже установлен."
    exit 0
fi

mkdir -p "$LAB2_DIR/wildfly"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

echo "Скачивание WildFly..."
curl --fail --location --progress-bar "$DOWNLOAD_URL" \
    --output "$TEMP_DIR/$WILDFLY_NAME.tar.gz"

tar -xzf "$TEMP_DIR/$WILDFLY_NAME.tar.gz" -C "$LAB2_DIR/wildfly"
echo "WildFly установлен в $WILDFLY_DIR"
