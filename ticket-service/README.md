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
