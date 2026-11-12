
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CookieValue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")
@Validated
public interface TicketsApi {

    @Operation(summary = "Добавить новый билет", description = "Создаёт новый объект Ticket и добавляет его в коллекцию", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Билет успешно создан", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Ticket.class))),

            @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: обязательный query-параметр ticket отсутствует, содержит некорректное percent-encoding или не является синтаксически корректным JSON.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "422", description = "Обязательные поля Ticket нарушены их типы, диапазоны, значения enum или максимальная длина comment.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets",
            produces = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<Ticket> createTicket(@NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с изменяемыми полями Ticket, закодированный в URL. id, creationDate и event.id не передаются (генерируются сервером). event может отсутствовать или быть null. Все остальные изменяемые поля обязательны. Swagger UI сериализует JSON и кодирует значение параметра автоматически. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketInput ticket
    );


    @Operation(summary = "Удалить билет", description = "Удаляет билет с указанным id. Если билет уже удалён или никогда не существовал, также возвращает 204 без тела. Повторный вызов сохраняет состояние отсутствия билета.", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Билет отсутствует в коллекции: удалён этим запросом или отсутствовал до запроса. Тело ответа отсутствует."),

            @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: path-параметр id невозможно преобразовать в int64, например, передано буквенное или дробное значение либо число за пределами int64.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "422", description = "Параметр id должен быть целым числом int64 больше 0.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    ResponseEntity<Void> deleteTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                      )) @PathVariable("id") Long id
    );


    @Operation(summary = "Получить билет по идентификатору", description = "Возвращает билет по идентификатору. Если значение If-None-Match совпадает с текущим ETag билета, сервис возвращает 304 без тела. ", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Билет успешно найден", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Ticket.class))),

            @ApiResponse(responseCode = "304", description = "Билет не изменился с момента предыдущего запроса"),

            @ApiResponse(responseCode = "404", description = "В коллекции билетов отсутствует билет с указанным идентификатором.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "422", description = "Параметр id должен быть целым числом int64 больше 0.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/{id}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<Ticket> getTicketById(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                         )) @PathVariable("id") Long id
            , @Parameter(in = ParameterIn.HEADER, description = "ETag, полученный в предыдущем ответе", schema = @Schema()) @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    );


    @Operation(summary = "Получить список билетов", description = "Возвращает массив билетов", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Массив билетов успешно получен", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Ticket.class)))),

            @ApiResponse(responseCode = "422", description = "page и size должны быть целыми числами в допустимых диапазонах; sort и filter должны содержать поддерживаемые поля, направления и операторы.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<List<Ticket>> getTickets(@Min(0) @Parameter(in = ParameterIn.QUERY, description = "Номер страницы с 0. Фильтрация и сортировка выполняются до пагинации. За пределами выборки возвращается пустой массив.", schema = @Schema(allowableValues = {"0"}
                                                    , defaultValue = "0")) @Valid @RequestParam(value = "page", required = false, defaultValue = "0") Integer page
            , @Min(1) @Max(100) @Parameter(in = ParameterIn.QUERY, description = "Максимальное количество элементов на странице.", schema = @Schema(allowableValues = {"1", "100"}, minimum = "1", maximum = "100"
                    , defaultValue = "10")) @Valid @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
            , @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Сортировка задаётся в формате `поле,направление`, где направление — `asc` или `desc`. Для сортировки по нескольким полям параметр передаётся несколько раз: `?sort=price,desc&sort=name,asc`. Вложенные поля записываются через точку, например `coordinates.x` или `event.name`. ", schema = @Schema()) @Valid @RequestParam(value = "sort", required = false) List<String> sort
            , @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Условие задаётся в формате `поле:оператор:значение`. Несколько параметров filter объединяются операцией AND. Поддерживаются операторы `eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `contains` и `isnull`. Вложенные поля записываются через точку. Пример: `?filter=price:gte:100&filter=type:eq:VIP`. ", schema = @Schema()) @Valid @RequestParam(value = "filter", required = false) List<String> filter
    );


    @Operation(summary = "Получить разрешённые методы коллекции", description = "Возвращает Allow со списком методов для /tickets. Это описание возможностей ресурса, не настройка CORS.", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Методы перечислены в Allow; тело отсутствует.")})
    @RequestMapping(value = "/tickets",
            method = RequestMethod.OPTIONS)
    ResponseEntity<Void> optionsTickets();


    @Operation(summary = "Частично обновить билет", description = "Обновляет только переданные поля билета. Остальные поля сохраняют прежние значения. Поля id, creationDate и event.id изменять нельзя.", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Билет успешно частично обновлён", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Ticket.class))),

            @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: path-параметр id невозможно преобразовать в int64 либо обязательный query-параметр ticket отсутствует, содержит некорректное percent-encoding или не является синтаксически корректным JSON.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "404", description = "В коллекции билетов отсутствует билет с указанным идентификатором.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "422", description = "id должен быть целым int64 больше 0, а переданные поля должны соответствовать схеме TicketPatch.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/{id}",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    ResponseEntity<Ticket> patchTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                       )) @PathVariable("id") Long id
            , @NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с полями Ticket, которые требуется изменить, закодированный в URL. Необходимо передать хотя бы одно поле. id, creationDate и event.id изменять нельзя. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketPatch ticket
    );


    @Operation(summary = "Обновить билет", description = "Полностью обновляет изменяемые поля билета с указанным идентификатором. id и creationDate сохраняются. Если поля event совпадают с текущими, event.id сохраняется; иначе сервер создаёт мероприятие с новым event.id. Если event отсутствует или равен null, связь удаляется. ", tags = {"Tickets"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Билет успешно обновлён", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Ticket.class))),

            @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: path-параметр id невозможно преобразовать в int64 либо обязательный query-параметр ticket отсутствует, содержит некорректное percent-encoding или не является синтаксически корректным JSON.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "404", description = "В коллекции билетов отсутствует билет с указанным идентификатором.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

            @ApiResponse(responseCode = "422", description = "id должен быть целым int64 больше 0, а поля Ticket должны соответствовать ограничениям схемы.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/{id}",
            produces = {"application/json"},
            method = RequestMethod.PUT)
    ResponseEntity<Ticket> updateTicket(@Min(1L) @Parameter(in = ParameterIn.PATH, description = "Уникальный идентификатор билета", required = true, schema = @Schema(allowableValues = {"1"}, minimum = "1"
                                        )) @PathVariable("id") Long id
            , @NotNull @Parameter(in = ParameterIn.QUERY, description = "JSON-объект с изменяемыми полями Ticket, закодированный в URL. id, creationDate и event.id не передаются (генерируются сервером). event может отсутствовать или быть null. Все остальные изменяемые поля обязательны. Swagger UI сериализует JSON и кодирует значение параметра автоматически. ", required = true, schema = @Schema()) @Valid @RequestParam(value = "ticket", required = true) TicketInput ticket
    );

}

