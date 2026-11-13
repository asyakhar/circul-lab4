# Mule Ticket REST Adapter

This Mule 4 application restores the Ticket Service REST API from laboratory
work 3 and delegates requests to the SOAP Ticket Service.

The adapter keeps the laboratory work 3 REST contract and performs only
transport-level conversion between REST/JSON and the SOAP Ticket Service.

## Requirements

- JDK 17;
- Maven 3.9+;
- Mule Runtime 4.9.x Enterprise Edition.

## Configuration

Default values are defined in `src/main/resources/application.properties`:

| Property | Default | Purpose |
| --- | --- | --- |
| `http.host` | `0.0.0.0` | REST listener interface |
| `http.port` | `8083` | REST listener port |
| `ticket.soap.wsdl` | `http://127.0.0.1:8081/ticket-service/ws/tickets.wsdl` | Ticket WSDL URL |
| `ticket.soap.endpoint` | `http://127.0.0.1:8081/ticket-service/ws` | SOAP request URL |

Every property can be overridden with a Mule command-line property, for
example:

```bash
./bin/mule \
  -M-Dhttp.port=18083 \
  -M-Dticket.soap.wsdl=http://127.0.0.1:18081/ticket-service/ws/tickets.wsdl \
  -M-Dticket.soap.endpoint=http://127.0.0.1:18081/ticket-service/ws
```

## Build

```bash
mvn clean package
```

The deployable Mule application is created in `target/`.

## Current endpoints

```text
GET /health
GET /api/v1/tickets
GET /api/v1/tickets/{id}
ANY /api/v1/*
```

The ticket collection route preserves `page`, `size`, repeated `sort`, and
repeated `filter` query parameters. The item route preserves the legacy `ETag`
and `If-None-Match` behaviour. SOAP Fault mapping and the remaining write and
query routes are added in the following integration blocks. Unknown routes
under `/api/v1/*` return a JSON `404` response using the legacy error shape.
