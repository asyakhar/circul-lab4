package ru.ifmo.soa.booking.api;

import ru.ifmo.soa.booking.model.*;
import ru.ifmo.soa.booking.api.PersonApiService;

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
@Path("/person")


@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public class PersonApi  {

    @Inject
    PersonApiService service;

    @DELETE
    @Path("/{person-id}/cancel")
    @Produces({ "application/json" })
    @Operation(summary = "Отменить все бронирования человека", description = "Отменяет все бронирования указанного человека, удаляя его идентификатор из всех связанных билетов через API первого сервиса. ", tags={ "Booking" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Все бронирования человека успешно отменены"),

        @ApiResponse(responseCode = "400", description = "Сервер не смог разобрать запрос: path-параметр person-id невозможно преобразовать в int64, например, передано буквенное или дробное значение либо число за пределами int64.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),

        @ApiResponse(responseCode = "422", description = "Параметр person-id должен быть целым числом int64 больше 0.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) })
    public Response cancelPersonBookings( @Min(1L) @PathParam("person-id") Long personId,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.cancelPersonBookings(personId,securityContext);
    }
}
