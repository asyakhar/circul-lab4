import os
import smtplib
import ssl
from email.errors import HeaderParseError
from email.headerregistry import Address
from email.message import EmailMessage


def email_address(value):
    if not isinstance(value, str) or "\r" in value or "\n" in value:
        raise ValueError("Invalid email address")
    value = value.strip()
    if len(value) > 254 or not value.isascii() or any(char.isspace() for char in value):
        raise ValueError("Invalid email address")
    try:
        address = Address(addr_spec=value)
    except (ValueError, HeaderParseError) as error:
        raise ValueError("Invalid email address") from error
    if not address.username or "." not in address.domain or address.addr_spec != value:
        raise ValueError("Invalid email address")
    return address


def send_ticket_email(ticket_id, recipient, pdf):
    host = os.environ["SMTP_HOST"]
    sender = email_address(os.environ.get("SMTP_FROM"))
    security = os.getenv("SMTP_SECURITY", "starttls")
    username = os.getenv("SMTP_USERNAME")
    password = os.getenv("SMTP_PASSWORD")
    if security not in {"starttls", "ssl", "plain"} or bool(username) != bool(password):
        raise ValueError("Invalid SMTP configuration")
    if security == "plain" and (host not in {"localhost", "127.0.0.1", "::1"} or username):
        raise ValueError("Plain SMTP is only available for local mail capture")

    default_ports = {"starttls": "587", "ssl": "465", "plain": "1025"}
    port = int(os.getenv("SMTP_PORT", default_ports[security]))
    if not 1 <= port <= 65535:
        raise ValueError("Invalid SMTP port")

    message = EmailMessage()
    message["From"] = sender
    message["To"] = recipient
    message["Subject"] = f"ЦИРКуль — билет №{ticket_id}"
    message.set_content(f"Ваш билет №{ticket_id} во вложении. Сохраните PDF для предъявления при входе.")
    message.add_attachment(pdf, maintype="application", subtype="pdf",
                           filename=f"circul-ticket-{ticket_id}.pdf")

    tls_context = ssl.create_default_context() if security != "plain" else None
    connection = (
        smtplib.SMTP_SSL(host, port, timeout=10, context=tls_context)
        if security == "ssl"
        else smtplib.SMTP(host, port, timeout=10)
    )
    try:
        if security == "starttls":
            connection.starttls(context=tls_context)
        if username:
            connection.login(username, password)
        connection.send_message(message)
    finally:
        connection.close()
