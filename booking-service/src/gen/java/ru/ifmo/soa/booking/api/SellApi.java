package ru.ifmo.soa.booking.api;

import ru.ifmo.soa.booking.model.*;
import ru.ifmo.soa.booking.api.SellApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import ru.ifmo.soa.booking.model.ErrorResponse;

import java.util.Map;
import java.util.List;
import ru.ifmo.soa.booking.api.NotFoundException;

import java.io.InputStream;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.*;
import jakarta.inject.Inject;

import jakarta.validation.constraints.*;
import org.springframework.stereotype.Component;

@Component
@Path("/sell")
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public class SellApi  {

    @Inject
    SellApiService service;

    @POST
    @Path("/{ticket-id}/{person-id}/{price}")
    @Produces({ "application/json" })
    @Operation(summary = "Продать билет", description = "Продаёт указанный билет указанному человеку за переданную сумму.         Для выполнения операции используется API первого сервиса. ", tags={ "Booking" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Билет успешно продан"),

        @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: ticket-id или person-id невозможно преобразовать в int64 либо price невозможно преобразовать в число double.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "404", description = "В коллекции билетов отсутствует билет с указанным идентификатором.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "409", description = "Для ticket-id уже существует связь с владельцем, включая того же person-id. Повторная продажа не выполняется.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "422", description = "ticket-id и person-id должны быть целыми int64 больше 0, price — числом больше 0.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
    public Response sellTicket( @Min(1L) @PathParam("ticket-id") Long ticketId, @Min(1L) @PathParam("person-id") Long personId, @DecimalMin(value = "0", inclusive = false) @PathParam("price") Double price,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.sellTicket(ticketId,personId,price,securityContext);
    }
}
