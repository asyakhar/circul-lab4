import os

import requests
import urllib3
from flask import Flask, Response, jsonify, render_template, request
try:
    from opentelemetry import trace
except ImportError:
    trace = None


app = Flask(__name__)
app.config["TEMPLATES_AUTO_RELOAD"] = True

TICKET_SERVICE_URL = os.getenv(
    "TICKET_SERVICE_URL",
    "http://localhost:8080/api/v1",
).rstrip("/")

BOOKING_SERVICE_URL = os.getenv(
    "BOOKING_SERVICE_URL",
    "http://localhost:8080/booking",
).rstrip("/")

CLIENT_CERT_FILE = os.getenv("CLIENT_CERT_FILE") or os.getenv("CLIENT_TLS_CERT")
CLIENT_KEY_FILE = os.getenv("CLIENT_KEY_FILE") or os.getenv("CLIENT_TLS_KEY")

HTTP_METHODS = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
session = requests.Session()

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)


@app.after_request
def expose_trace_id(response):
    """Make a completed browser request easy to find in Jaeger."""
    if trace is None:
        return response
    span_context = trace.get_current_span().get_span_context()
    if span_context.is_valid:
        response.headers["X-Trace-Id"] = format(span_context.trace_id, "032x")
    return response


@app.get("/")
def index():
    return render_template("index.html")


def proxy_request(base_url, resource):
    url = f"{base_url}/{resource}"
    headers = {}

    for header_name in ("Accept", "Content-Type", "If-None-Match"):
        value = request.headers.get(header_name)
        if value:
            headers[header_name] = value

    try:
        backend_response = session.request(
            method=request.method,
            url=url,
            params=list(request.args.items(multi=True)),
            data=request.get_data() or None,
            headers=headers,
            timeout=10,
            verify=False,
        )
    except requests.RequestException:
        return jsonify({
            "status": 424,
            "error": "Failed Dependency",
            "message": "Не удалось подключиться к веб-сервису.",
            "path": request.path,
        }), 424

    response_headers = {}
    for header_name in (
        "Content-Type",
        "Location",
        "ETag",
        "Allow",
        "X-Booking-Instance",
    ):
        value = backend_response.headers.get(header_name)
        if value:
            response_headers[header_name] = value

    return Response(
        backend_response.content,
        status=backend_response.status_code,
        headers=response_headers,
    )


@app.route("/api/ticket/<path:resource>", methods=HTTP_METHODS)
def ticket_proxy(resource):
    return proxy_request(TICKET_SERVICE_URL, resource)


@app.route("/api/booking/<path:resource>", methods=HTTP_METHODS)
def booking_proxy(resource):
    return proxy_request(BOOKING_SERVICE_URL, resource)


if __name__ == "__main__":
    port = int(os.getenv("CLIENT_PORT", "5000"))
    if bool(CLIENT_CERT_FILE) != bool(CLIENT_KEY_FILE):
        raise RuntimeError(
            "TLS certificate and key must be set together"
        )
    ssl_context = (
        (CLIENT_CERT_FILE, CLIENT_KEY_FILE)
        if CLIENT_CERT_FILE and CLIENT_KEY_FILE
        else None
    )
    app.run(
        host="0.0.0.0",
        port=port,
        debug=False,
        ssl_context=ssl_context,
    )
