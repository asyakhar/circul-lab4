package ru.ifmo.soa.booking.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        ConstraintViolation<?> violation = exception
                .getConstraintViolations()
                .stream()
                .findFirst()
                .orElse(null);

        String field = violation == null
                ? "parameter"
                : normalizeField(extractField(
                        violation.getPropertyPath().toString()
                ));
        Object value = violation == null
                ? null
                : violation.getInvalidValue();
        String message = createMessage(field, value);

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponses.create(
                        422,
                        "Unprocessable Content",
                        message,
                        uriInfo.getRequestUri().getPath(),
                        field
                ))
                .build();
    }

    private String extractField(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot >= 0
                ? propertyPath.substring(dot + 1)
                : propertyPath;
    }

    private String normalizeField(String field) {
        return switch (field) {
            case "ticketId" -> "ticket-id";
            case "personId" -> "person-id";
            default -> field;
        };
    }

    private String createMessage(String field, Object value) {
        if (field.equals("price")) {
            return "price должен быть числом больше 0; получено "
                    + value + ".";
        }
        return field + " должен быть целым числом больше 0; получено "
                + value + ".";
    }
}
