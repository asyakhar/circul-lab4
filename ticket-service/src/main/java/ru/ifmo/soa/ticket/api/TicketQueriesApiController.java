package ru.ifmo.soa.ticket.api;

import ru.ifmo.soa.ticket.model.ErrorResponse;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")
@RestController
public class TicketQueriesApiController implements TicketQueriesApi {

    private final TicketQueriesApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketQueriesApiController(TicketQueriesApiDelegate delegate) {
        this.delegate = delegate;
    }

    public ResponseEntity<Ticket> getTicketWithMaxType() {
        return delegate.getTicketWithMaxType();
    }

    public ResponseEntity<List<Ticket>> getTicketsByCommentSubstring(@NotNull @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Подстрока, которую должно содержать поле comment", required = true, schema = @Schema()) @Valid @RequestParam(value = "substring", required = true) String substring
    ) {
        return delegate.getTicketsByCommentSubstring(substring);
    }

    public ResponseEntity<List<Ticket>> getTicketsWithTypeGreaterThan(@NotNull @Parameter(in = ParameterIn.QUERY, description = "Тип билета, с которым выполняется сравнение", required = true, schema = @Schema()) @Valid @RequestParam(value = "type", required = true) TicketType type
    ) {
        return delegate.getTicketsWithTypeGreaterThan(type);
    }

}
