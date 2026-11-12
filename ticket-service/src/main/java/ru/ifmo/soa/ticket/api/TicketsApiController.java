package ru.ifmo.soa.ticket.api;

import ru.ifmo.soa.ticket.model.ErrorResponse;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;
import ru.ifmo.soa.ticket.model.TicketPatch;
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
public class TicketsApiController implements TicketsApi {

    private final TicketsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketsApiController(TicketsApiDelegate delegate) {
        this.delegate = delegate;
    }

    public ResponseEntity<Ticket> createTicket(@NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с изменяемыми полями Ticket, закодированный в URL. id, creationDate и event.id не передаются (генерируются сервером). event может отсутствовать или быть null. Все остальные изменяемые поля обязательны. Swagger UI сериализует JSON и кодирует значение параметра автоматически. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketInput ticket
    ) {
        return delegate.createTicket(ticket);
    }

    public ResponseEntity<Void> deleteTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                             )) @PathVariable("id") Long id
    ) {
        return delegate.deleteTicket(id);
    }

    public ResponseEntity<Ticket> getTicketById(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                                )) @PathVariable("id") Long id
            , @Parameter(in = ParameterIn.HEADER, description = "ETag, полученный в предыдущем ответе", schema = @Schema()) @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) {
        return delegate.getTicketById(id, ifNoneMatch);
    }

    public ResponseEntity<List<Ticket>> getTickets(@Min(0) @Parameter(in = ParameterIn.QUERY, description = "Номер страницы с 0. Фильтрация и сортировка выполняются до пагинации. За пределами выборки возвращается пустой массив.", schema = @Schema(allowableValues = {"0"}
                                                           , defaultValue = "0")) @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page
            , @Min(1) @Max(100) @Parameter(in = ParameterIn.QUERY, description = "Максимальное количество элементов на странице.", schema = @Schema(allowableValues = {"1", "100"}, minimum = "1", maximum = "100"
                    , defaultValue = "10")) @Valid @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
            , @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Сортировка задаётся в формате `поле,направление`, где направление — `asc` или `desc`. Для сортировки по нескольким полям параметр передаётся несколько раз: `?sort=price,desc&sort=name,asc`. Вложенные поля записываются через точку, например `coordinates.x` или `event.name`. ", schema = @Schema()) @Valid @RequestParam(value = "sort", required = false) List<String> sort
            , @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Условие задаётся в формате `поле:оператор:значение`. Несколько параметров filter объединяются операцией AND. Поддерживаются операторы `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `contains` и `isnull`. Вложенные поля записываются через точку. Пример: `?filter=price:gte:100&filter=type:eq:VIP`. ", schema = @Schema()) @Valid @RequestParam(value = "filter", required = false) List<String> filter
    ) {
        return delegate.getTickets(page, size, sort, filter);
    }

    public ResponseEntity<Void> optionsTickets() {
        return delegate.optionsTickets();
    }

    public ResponseEntity<Ticket> patchTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                              )) @PathVariable("id") Long id
            , @NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с полями Ticket, которые требуется изменить, закодированный в URL. Необходимо передать хотя бы одно поле. id, creationDate и event.id изменять нельзя. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketPatch ticket
    ) {
        return delegate.patchTicket(id, ticket);
    }

    public ResponseEntity<Ticket> updateTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                               )) @PathVariable("id") Long id
            , @NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с изменяемыми полями Ticket, закодированный в URL. id, creationDate и event.id не передаются (генерируются сервером). event может отсутствовать или быть null. Все остальные изменяемые поля обязательны. Swagger UI сериализует JSON и кодирует значение параметра автоматически. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketInput ticket
    ) {
        return delegate.updateTicket(id, ticket);
    }

}
