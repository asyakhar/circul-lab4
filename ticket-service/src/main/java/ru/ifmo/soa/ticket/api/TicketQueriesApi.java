
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
public interface TicketQueriesApi {

    @Operation(summary = "Получить билет с максимальным типом", description = "Возвращает один любой билет, поле `type` которого имеет максимальное значение среди всех билетов коллекции.  Значения сравниваются в порядке объявления перечисления: `VIP < USUAL < BUDGETARY < CHEAP`.  Если максимальный тип имеют несколько билетов, сервис может вернуть любой из них. ", tags = {"Ticket queries"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Билет с максимальным значением поля type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Ticket.class))),

            @ApiResponse(responseCode = "404", description = "Коллекция билетов пуста, поэтому определить билет с максимальным type невозможно.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/max-type", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Ticket> getTicketWithMaxType();


    @Operation(summary = "Найти билеты по подстроке в комментарии", description = "Возвращает массив билетов, поле \"comment\" которых содержит заданную подстроку ", tags = {"Ticket queries"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Массив найденных билетов", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Ticket.class)))),

            @ApiResponse(responseCode = "422", description = "Обязательный параметр substring отсутствует или является пустой строкой.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/comment-contains", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<List<Ticket>> getTicketsByCommentSubstring(@NotNull @Size(min = 1) @Parameter(in = ParameterIn.QUERY, description = "Подстрока, которую должно содержать поле comment", required = true, schema = @Schema()) @Valid @RequestParam(value = "substring", required = true) String substring
    );


    @Operation(summary = "Получить билеты с типом больше заданного", description = "Возвращает массив билетов, значение поля `type` которых больше переданного значения ", tags = {"Ticket queries"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Массив билетов с типом больше заданного", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Ticket.class)))),

            @ApiResponse(responseCode = "422", description = "Обязательный параметр type отсутствует или не равен VIP, USUAL, BUDGETARY либо CHEAP.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))})
    @RequestMapping(value = "/tickets/type-greater-than",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<List<Ticket>> getTicketsWithTypeGreaterThan(@NotNull @Parameter(in = ParameterIn.QUERY, description = "Тип билета, с которым выполняется сравнение", required = true, schema = @Schema()) @Valid @RequestParam(value = "type", required = true) TicketType type
    );

}

