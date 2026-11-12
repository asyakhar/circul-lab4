package ru.ifmo.soa.booking.service;

import ru.ifmo.soa.booking.model.ErrorResponse;
import ru.ifmo.soa.booking.model.ErrorResponseViolations;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ErrorResponses {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private ErrorResponses() {
    }

    public static ErrorResponse create(
            int status,
            String error,
            String message,
            String path
    ) {
        return create(status, error, message, path, null);
    }

    public static ErrorResponse create(
            int status,
            String error,
            String message,
            String path,
            String field
    ) {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp(
                LocalDateTime.now(ZoneOffset.UTC).format(DATE_FORMAT)
        );
        response.setStatus(status);
        response.setError(error);
        response.setMessage(message);
        response.setPath(path);

        if (field != null) {
            ErrorResponseViolations violation =
                    new ErrorResponseViolations();
            violation.setField(field);
            violation.setMessage(message);
            response.setViolations(List.of(violation));
        }

        return response;
    }
}
