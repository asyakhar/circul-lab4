# SOAP Ticket Service

`ticket-service` is the SOAP version of the ticket service for laboratory work 4.
The service is packaged as a WAR and is intended to run on WildFly.

## Build and test

Requirements:

- JDK 17;
- Maven 3.9+.

```bash
mvn clean test package
```

The resulting archive is `target/ticket-service-1.0.0.war`.

## SOAP contract

The source XSD is located at `src/main/resources/xsd/tickets.xsd`. JAXB classes
are generated from it during the Maven build.

After deploying the WAR, the WSDL is available at:

```text
https://localhost:8543/ticket-service/ws/tickets.wsdl
```

The SOAP endpoint is:

```text
https://localhost:8543/ticket-service/ws
```

An example `createTicketRequest` envelope is available at
`examples/create-ticket.xml`.

## Local WildFly deployment

With the local WildFly instance prepared, deploy the built WAR using:

```bash
./scripts/deploy-ticket-soap-local.sh
```

From the repository root, verify the WSDL and send the example request:

```bash
curl -kfsS https://localhost:8543/ticket-service/ws/tickets.wsdl

curl -kfsS \
  -H 'Content-Type: text/xml; charset=UTF-8' \
  --data-binary @ticket-service/examples/create-ticket.xml \
  https://localhost:8543/ticket-service/ws
```

Application errors are returned as SOAP Faults. Their `detail` element contains
`status`, `code`, `message`, and, when applicable, `field`. The Mule REST facade
uses these values to restore the HTTP error contract from laboratory work 3.

## MTOM PDF ticket extension

The optional `downloadTicket` operation accepts a ticket ID and returns a
designed PDF ticket as an MTOM/XOP attachment. The SOAP body contains the file
name, MIME type, byte size, and an `xop:Include` reference; the PDF bytes travel
as a separate MIME part instead of Base64 embedded in XML.

The control stub contains a real Code 128 barcode. It encodes
`CIRCUL-XXXXXXXX`, where `XXXXXXXX` is the zero-padded ticket ID. The automated
test renders the final PDF and decodes this barcode back to its original value.

For local demonstration, Mule exposes the attachment as:

```text
GET /api/v1/tickets/{id}/pdf
```

To show the raw MTOM response directly from SOAP (ticket `1` in this example):

```bash
./scripts/inspect-mtom-pdf.sh 1
```

The command prints the `Multipart/Related` content type, `xop:Include`, the PDF
MIME part, and the `%PDF` signature.

To produce the visual test artifact during the test run:

```bash
mvn test -Dticket.pdf.preview=../output/pdf/sample-mtom-ticket.pdf
```
