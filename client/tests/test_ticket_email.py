import os
import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch


CLIENT_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(CLIENT_DIR))

import app as client_app
from ticket_mail import email_address, send_ticket_email


class EmailAddressTest(unittest.TestCase):
    def test_accepts_regular_address(self):
        address = email_address("customer@example.com")

        self.assertEqual(address.addr_spec, "customer@example.com")

    def test_rejects_header_injection(self):
        with self.assertRaises(ValueError):
            email_address("customer@example.com\nBcc: attacker@example.com")

    def test_rejects_address_without_domain(self):
        with self.assertRaises(ValueError):
            email_address("customer@localhost")


class SendTicketEmailTest(unittest.TestCase):
    @patch.dict(
        os.environ,
        {
            "SMTP_HOST": "127.0.0.1",
            "SMTP_PORT": "1025",
            "SMTP_SECURITY": "plain",
            "SMTP_FROM": "tickets@example.test",
        },
        clear=True,
    )
    @patch("ticket_mail.smtplib.SMTP")
    def test_sends_pdf_attachment(self, smtp):
        connection = smtp.return_value

        send_ticket_email(
            42,
            email_address("customer@example.com"),
            b"%PDF-1.7 test ticket",
        )

        smtp.assert_called_once_with("127.0.0.1", 1025, timeout=10)
        connection.send_message.assert_called_once()
        message = connection.send_message.call_args.args[0]
        self.assertEqual(str(message["From"]), "tickets@example.test")
        self.assertEqual(str(message["To"]), "customer@example.com")
        self.assertEqual(message["Subject"], "ЦИРКуль — билет №42")
        attachments = list(message.iter_attachments())
        self.assertEqual(len(attachments), 1)
        self.assertEqual(attachments[0].get_filename(), "circul-ticket-42.pdf")
        self.assertEqual(attachments[0].get_content(), b"%PDF-1.7 test ticket")
        connection.close.assert_called_once()


class EmailTicketEndpointTest(unittest.TestCase):
    def setUp(self):
        client_app.app.config.update(TESTING=True)
        self.client = client_app.app.test_client()

    @staticmethod
    def pdf_response(status=200, content=b"%PDF-1.7 generated ticket"):
        response = MagicMock()
        response.status_code = status
        response.headers = {"Content-Type": "application/pdf"}
        response.content = content
        context = MagicMock()
        context.__enter__.return_value = response
        return context

    @patch.dict(
        os.environ,
        {"SMTP_HOST": "127.0.0.1", "SMTP_FROM": "tickets@example.test"},
        clear=False,
    )
    @patch.object(client_app, "send_ticket_email")
    @patch.object(client_app.session, "get")
    def test_downloads_pdf_and_sends_it(self, get, send):
        get.return_value = self.pdf_response()

        response = self.client.post(
            "/api/tickets/7/email",
            json={"email": "customer@example.com"},
            base_url="http://localhost",
        )

        self.assertEqual(response.status_code, 200)
        get.assert_called_once_with(
            f"{client_app.TICKET_SERVICE_URL}/tickets/7/pdf",
            timeout=10,
            allow_redirects=False,
        )
        recipient = send.call_args.args[1]
        self.assertEqual(recipient.addr_spec, "customer@example.com")
        self.assertEqual(send.call_args.args[2], b"%PDF-1.7 generated ticket")

    @patch.dict(
        os.environ,
        {"SMTP_HOST": "127.0.0.1", "SMTP_FROM": "tickets@example.test"},
        clear=False,
    )
    def test_rejects_invalid_recipient(self):
        response = self.client.post(
            "/api/tickets/7/email",
            json={"email": "not-an-email"},
            base_url="http://localhost",
        )

        self.assertEqual(response.status_code, 400)

    @patch.dict(os.environ, {}, clear=True)
    def test_requires_smtp_configuration(self):
        response = self.client.post(
            "/api/tickets/7/email",
            json={"email": "customer@example.com"},
            base_url="http://localhost",
        )

        self.assertEqual(response.status_code, 503)

    @patch.dict(
        os.environ,
        {"SMTP_HOST": "127.0.0.1", "SMTP_FROM": "tickets@example.test"},
        clear=False,
    )
    def test_rejects_remote_request(self):
        response = self.client.post(
            "/api/tickets/7/email",
            json={"email": "customer@example.com"},
            base_url="http://localhost",
            environ_base={"REMOTE_ADDR": "203.0.113.10"},
        )

        self.assertEqual(response.status_code, 403)

    @patch.dict(
        os.environ,
        {"SMTP_HOST": "127.0.0.1", "SMTP_FROM": "tickets@example.test"},
        clear=False,
    )
    @patch.object(client_app.session, "get")
    def test_preserves_not_found(self, get):
        get.return_value = self.pdf_response(status=404, content=b"")

        response = self.client.post(
            "/api/tickets/999/email",
            json={"email": "customer@example.com"},
            base_url="http://localhost",
        )

        self.assertEqual(response.status_code, 404)


if __name__ == "__main__":
    unittest.main()
