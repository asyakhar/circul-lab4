#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OBSERVABILITY_DIR="$PROJECT_DIR/tools/observability"

JAEGER_VERSION="2.22.0"
OTEL_JAVA_AGENT_VERSION="2.30.0"

case "$(uname -s)" in
    Linux) JAEGER_OS="linux" ;;
    Darwin) JAEGER_OS="darwin" ;;
    *)
        echo "Неподдерживаемая операционная система: $(uname -s)" >&2
        exit 1
        ;;
esac

case "$(uname -m)" in
    x86_64|amd64) JAEGER_ARCH="amd64" ;;
    aarch64|arm64) JAEGER_ARCH="arm64" ;;
    *)
        echo "Неподдерживаемая архитектура: $(uname -m)" >&2
        exit 1
        ;;
esac

if ! command -v tar >/dev/null 2>&1; then
    echo "Не найдена команда 'tar'." >&2
    exit 1
fi
if ! command -v wget >/dev/null 2>&1 \
        && ! command -v curl >/dev/null 2>&1; then
    echo "Нужна команда 'wget' или 'curl'." >&2
    exit 1
fi

download() {
    local url="$1"
    local output="$2"
    if command -v wget >/dev/null 2>&1; then
        wget -q --show-progress "$url" -O "$output"
    else
        curl -fL --retry 3 --silent --show-error "$url" -o "$output"
    fi
}

mkdir -p "$OBSERVABILITY_DIR"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

if [[ ! -x "$OBSERVABILITY_DIR/jaeger" ]] \
        || ! "$OBSERVABILITY_DIR/jaeger" --version >/dev/null 2>&1; then
    echo "Скачивание Jaeger $JAEGER_VERSION..."
    archive="$TEMP_DIR/jaeger.tar.gz"
    download \
        "https://github.com/jaegertracing/jaeger/releases/download/v$JAEGER_VERSION/jaeger-$JAEGER_VERSION-$JAEGER_OS-$JAEGER_ARCH.tar.gz" \
        "$archive"
    tar -xzf "$archive" -C "$TEMP_DIR"
    install -m 0755 \
        "$TEMP_DIR/jaeger-$JAEGER_VERSION-$JAEGER_OS-$JAEGER_ARCH/jaeger" \
        "$OBSERVABILITY_DIR/jaeger"
fi

if [[ ! -r "$OBSERVABILITY_DIR/opentelemetry-javaagent.jar" ]]; then
    echo "Скачивание OpenTelemetry Java Agent $OTEL_JAVA_AGENT_VERSION..."
    download \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$OTEL_JAVA_AGENT_VERSION/opentelemetry-javaagent.jar" \
        "$TEMP_DIR/opentelemetry-javaagent.jar"
    install -m 0644 "$TEMP_DIR/opentelemetry-javaagent.jar" \
        "$OBSERVABILITY_DIR/opentelemetry-javaagent.jar"
fi

echo "Инструменты наблюдаемости установлены в $OBSERVABILITY_DIR"
