package ru.ifmo.soa.booking.service;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper
        implements ExceptionMapper<BadRequestException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BadRequestException exception) {
        String field = findInvalidField(uriInfo.getPath());
        String message = field
                + " невозможно преобразовать в требуемый числовой тип.";

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponses.create(
                        400,
                        "Bad Request",
                        message,
                        uriInfo.getRequestUri().getPath(),
                        field
                ))
                .build();
    }

    private String findInvalidField(String relativePath) {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        if (relativePath.startsWith("booking/")) {
            relativePath = relativePath.substring("booking/".length());
        }
        String[] parts = relativePath.split("/");

        if (parts.length == 4 && parts[0].equals("sell")) {
            if (!isLong(parts[1])) {
                return "ticket-id";
            }
            if (!isLong(parts[2])) {
                return "person-id";
            }
            if (!isDouble(parts[3])) {
                return "price";
            }
        }

        if (parts.length == 3 && parts[0].equals("person")) {
            return "person-id";
        }

        return "path parameter";
    }

    private boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
