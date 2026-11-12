package ru.ifmo.soa.ticket.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.ifmo.soa.ticket.model.ErrorResponse;
import ru.ifmo.soa.ticket.model.ErrorResponseViolations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private static final Set<String> UNPROCESSABLE_PARAMETERS =
            Set.of("page", "size", "sort", "filter", "substring", "type");

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            TicketNotFoundException exception,
            HttpServletRequest request
    ) {
        return createResponse(
                404,
                "Not Found",
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(EmptyTicketCollectionException.class)
    public ResponseEntity<ErrorResponse> handleEmptyCollection(
            EmptyTicketCollectionException exception,
            HttpServletRequest request
    ) {
        return createResponse(
                404,
                "Not Found",
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQuery(
            InvalidQueryException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponseViolations> violations = List.of(
                violation(exception.getField(), exception.getMessage())
        );

        return createResponse(
                422,
                "Unprocessable Content",
                exception.getMessage(),
                request,
                violations
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String field = exception.getParameterName();
        int status = field.equals("ticket") ? 400 : 422;

        String message = "Обязательный параметр "
                + field + " отсутствует.";

        return createResponse(
                status,
                status == 400
                        ? "Bad Request"
                        : "Unprocessable Content",
                message,
                request,
                List.of(violation(field, message))
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String field = exception.getName();
        Object value = exception.getValue();

        boolean isUnprocessable;

        if (field.equals("ticket")) {
            isUnprocessable =
                    !hasCause(exception, JsonParseException.class);
        } else {
            isUnprocessable =
                    UNPROCESSABLE_PARAMETERS.contains(field)
                            || field.equals("id")
                            && request.getMethod().equals("GET");
        }

        int status = isUnprocessable ? 422 : 400;

        String message;

        if (field.equals("ticket")) {
            message = status == 400
                    ? "Параметр ticket содержит неразбираемый JSON."
                    : "Параметр ticket не соответствует схеме.";
        } else {
            message = "Параметр " + field
                    + " содержит некорректное значение: "
                    + value + ".";
        }

        return createResponse(
                status,
                status == 400
                        ? "Bad Request"
                        : "Unprocessable Content",
                message,
                request,
                List.of(violation(field, message))
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponseViolations> violations =
                exception.getConstraintViolations()
                        .stream()
                        .map(this::convertViolation)
                        .toList();

        String message = violations.isEmpty()
                ? "Переданные данные не прошли проверку."
                : violations.get(0).getMessage();

        return createResponse(
                422,
                "Unprocessable Content",
                message,
                request,
                violations
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponseViolations> violations = new ArrayList<>();

        exception.getAllValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();

            result.getResolvableErrors().forEach(error -> {
                String message = error.getDefaultMessage();

                violations.add(violation(
                        field == null ? "parameter" : field,
                        message == null
                                ? "Некорректное значение."
                                : message
                ));
            });
        });

        String message = violations.isEmpty()
                ? "Переданные данные не прошли проверку."
                : violations.get(0).getMessage();

        return createResponse(
                422,
                "Unprocessable Content",
                message,
                request,
                violations
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorResponseViolations> violations =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error -> violation(
                                error.getField(),
                                error.getDefaultMessage()
                        ))
                        .toList();

        String message = violations.isEmpty()
                ? "Переданные данные не прошли проверку."
                : violations.get(0).getMessage();

        return createResponse(
                422,
                "Unprocessable Content",
                message,
                request,
                violations
        );
    }

    private boolean hasCause(
            Throwable exception,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = exception;

        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private ErrorResponseViolations convertViolation(
            ConstraintViolation<?> source
    ) {
        String path = source.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');

        String field = lastDot >= 0
                ? path.substring(lastDot + 1)
                : path;

        return violation(field, source.getMessage());
    }

    private ErrorResponseViolations violation(
            String field,
            String message
    ) {
        return new ErrorResponseViolations()
                .field(field)
                .message(message);
    }

    private ResponseEntity<ErrorResponse> createResponse(
            int status,
            String errorName,
            String message,
            HttpServletRequest request,
            List<ErrorResponseViolations> violations
    ) {
        ErrorResponse error = new ErrorResponse()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC).format(DATE_FORMAT))
                .status(status)
                .error(errorName)
                .message(message)
                .path(request.getRequestURI());

        if (violations != null && !violations.isEmpty()) {
            error.setViolations(violations);
        }

        return ResponseEntity.status(status).body(error);
    }
}