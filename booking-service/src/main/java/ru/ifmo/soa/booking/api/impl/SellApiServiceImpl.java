package ru.ifmo.soa.booking.api.impl;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.ifmo.soa.booking.api.NotFoundException;
import ru.ifmo.soa.booking.api.SellApiService;
import ru.ifmo.soa.booking.service.BookingStore;
import ru.ifmo.soa.booking.service.ErrorResponses;
import ru.ifmo.soa.booking.service.TicketServiceClient;
import org.springframework.stereotype.Service;

@Service
public class SellApiServiceImpl implements SellApiService {

    @Inject
    TicketServiceClient ticketServiceClient;

    @Inject
    BookingStore bookingStore;

    @Override
    public Response sellTicket(
            Long ticketId,
            Long personId,
            Double price,
            SecurityContext securityContext
    ) throws NotFoundException {
        String path = "/booking/sell/" + ticketId
                + "/" + personId + "/" + price;

        if (ticketId == null || personId == null || price == null
                || ticketId <= 0 || personId <= 0
                || !Double.isFinite(price) || price <= 0) {
            String message =
                    "ticket-id, person-id и price должны быть больше 0.";
            return error(422, "Unprocessable Content", message, path);
        }

        if (!ticketServiceClient.exists(ticketId)) {
            String message = "Билет с id " + ticketId + " не найден.";
            return error(404, "Not Found", message, path);
        }

        if (!bookingStore.sell(ticketId, personId, price)) {
            String message =
                    "Билет с id " + ticketId + " уже продан.";
            return error(409, "Conflict", message, path);
        }

        return Response.noContent().build();
    }

    private Response error(int status, String error, String message, String path) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponses.create(
                        status,
                        error,
                        message,
                        path
                ))
                .build();
    }
}
