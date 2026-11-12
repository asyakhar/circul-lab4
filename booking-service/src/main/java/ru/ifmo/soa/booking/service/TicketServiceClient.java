package ru.ifmo.soa.booking.service;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TicketServiceClient {

    private final Client client;
    private final String baseUrl;

    public TicketServiceClient(
            @Value("${ticket.service.url:http://localhost:8080/api/v1}")
            String configuredUrl
    ) {
        client = ClientBuilder.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        baseUrl = removeTrailingSlash(configuredUrl);
    }

    public boolean exists(long ticketId) {
        try (Response response = client.target(baseUrl)
                .path("tickets")
                .path(Long.toString(ticketId))
                .request()
                .get()) {
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                return true;
            }
            if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return false;
            }

            throw new TicketServiceCallException(
                    "Ticket Service вернул неожиданный HTTP-код "
                            + response.getStatus() + "."
            );
        } catch (TicketServiceCallException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TicketServiceCallException(
                    "Не удалось вызвать Ticket Service.",
                    exception
            );
        }
    }

    @PreDestroy
    public void close() {
        client.close();
    }

    private String removeTrailingSlash(String value) {
        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}
