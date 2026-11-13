# Mule Ticket REST Adapter

This Mule 4 application restores the Ticket Service REST API from laboratory
work 3 and delegates requests to the SOAP Ticket Service.

This initial module contains the application skeleton only. Ticket operations
and SOAP transformations are added in the following implementation blocks.

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
ANY /api/v1/*
```

`GET /health` returns the adapter status. Until the REST operations are added,
requests under `/api/v1/*` return a JSON `404` response using the legacy error
shape.
