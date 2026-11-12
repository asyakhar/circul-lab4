export async function callApi(url, options = {}) {
    const requestOptions = {...options};
    const requestedMethod = String(requestOptions.method || "GET").toUpperCase();

    // UserDir на se.ifmo.ru принимает PHP-запросы только через GET/POST/OPTIONS.
    // Для публичного прокси туннелируем остальные методы через POST.
    if (url.includes("proxy.php?") && ["DELETE", "PUT", "PATCH"].includes(requestedMethod)) {
        requestOptions.method = "POST";
        requestOptions.headers = {
            ...Object.fromEntries(new Headers(requestOptions.headers || {}).entries()),
            "X-HTTP-Method-Override": requestedMethod,
        };
    }

    const response = await fetch(url, requestOptions);
    const text = await response.text();
    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch (_) {
            data = text;
        }
    }

    if (!response.ok && response.status !== 304) {
        throw new Error(formatError(data, response.status));
    }

    return {response, data};
}

export function emitMessage(source, text, type = "success") {
    source.dispatchEvent(new CustomEvent("circus:message", {
        bubbles: true,
        detail: {text, type},
    }));
}

export function lines(value) {
    return value.split("\n").map(item => item.trim()).filter(Boolean);
}

export function escapeHtml(value) {
    return String(value ?? "—")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatError(data, status) {
    if (!data || typeof data !== "object") {
        return `Ошибка HTTP ${status}.`;
    }

    let text = data.message || data.error || `Ошибка HTTP ${status}.`;
    if (Array.isArray(data.violations) && data.violations.length > 0) {
        const violations = data.violations
            .map(item => `${item.field}: ${item.message}`)
            .join("; ");
        text += ` (${violations})`;
    }
    return text;
}
