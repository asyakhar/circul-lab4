import os
import smtplib
from ipaddress import ip_address
from urllib.parse import urlsplit

import requests
import urllib3
from flask import Flask, Response, jsonify, render_template, request
from ticket_mail import email_address, send_ticket_email
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
    email_enabled = all(os.getenv(name) for name in ("SMTP_HOST", "SMTP_FROM"))
    return render_template("index.html", ticket_email_enabled=email_enabled)


@app.post("/api/tickets/<int:ticket_id>/email")
def email_ticket(ticket_id):
    if (
        not request.remote_addr
        or not ip_address(request.remote_addr).is_loopback
        or urlsplit(request.host_url).hostname not in {"localhost", "127.0.0.1", "::1"}
    ):
        return jsonify(message="Отправка доступна в локальном клиенте."), 403
    if ticket_id == 0:
        return jsonify(message="Укажите положительный ID билета."), 400

    data = request.get_json(silent=True)
    try:
        recipient = email_address(data.get("email") if isinstance(data, dict) else None)
    except ValueError:
        return jsonify(message="Введите корректный адрес электронной почты."), 400

    if not os.getenv("SMTP_HOST") or not os.getenv("SMTP_FROM"):
        return jsonify(message="Отправка почты пока не настроена."), 503

    try:
        pdf_url = f"{TICKET_SERVICE_URL}/tickets/{ticket_id}/pdf"
        with session.get(pdf_url, timeout=10, allow_redirects=False) as pdf:
            if pdf.status_code == 404:
                return jsonify(message="Билет не найден."), 404
            mime_type = pdf.headers.get("Content-Type", "").partition(";")[0].strip().lower()
            if pdf.status_code != 200 or mime_type != "application/pdf" or not pdf.content.startswith(b"%PDF-"):
                return jsonify(message="Не удалось получить PDF билета."), 502
            content = pdf.content
    except requests.RequestException:
        return jsonify(message="Не удалось получить PDF билета."), 502

    try:
        send_ticket_email(ticket_id, recipient, content)
    except ValueError:
        app.logger.error("Invalid ticket email SMTP configuration")
        return jsonify(message="Проверьте настройки почтового сервера."), 503
    except (smtplib.SMTPException, OSError):
        app.logger.warning("SMTP failed to accept a ticket email")
        return jsonify(message="Почтовый сервер не подтвердил отправку письма."), 502

    return jsonify(message="Почтовый сервер принял письмо с PDF.")


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
        "Content-Disposition",
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
