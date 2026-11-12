package ru.ifmo.soa.booking.api.impl;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.ifmo.soa.booking.api.NotFoundException;
import ru.ifmo.soa.booking.api.PersonApiService;
import ru.ifmo.soa.booking.service.BookingStore;
import ru.ifmo.soa.booking.service.ErrorResponses;
import org.springframework.stereotype.Service;

@Service
public class PersonApiServiceImpl implements PersonApiService {

    @Inject
    BookingStore bookingStore;

    @Override
    public Response cancelPersonBookings(Long personId, SecurityContext securityContext) throws NotFoundException {
        String path = "/booking/person/" + personId + "/cancel";
        if (personId == null || personId <= 0) {
            String message = "person-id должен быть больше 0.";
            return Response.status(422)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ErrorResponses.create(
                            422,
                            "Unprocessable Content",
                            message,
                            path,
                            "person-id"
                    ))
                    .build();
        }
        bookingStore.cancelByPerson(personId);
        return Response.noContent().build();
    }
}
