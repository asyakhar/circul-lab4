package ru.ifmo.soa.ticket.api;

import ru.ifmo.soa.ticket.model.ErrorResponse;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;
import ru.ifmo.soa.ticket.model.TicketPatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")
public interface TicketsApiDelegate {

    ResponseEntity<Ticket> createTicket(TicketInput ticket);

    ResponseEntity<Void> deleteTicket(Long id);

    ResponseEntity<Ticket> getTicketById(Long id,
                                         String ifNoneMatch);

    ResponseEntity<List<Ticket>> getTickets(Integer page,
                                            Integer size,
                                            List<String> sort,
                                            List<String> filter);

    ResponseEntity<Void> optionsTickets();

    ResponseEntity<Ticket> patchTicket(Long id,
                                       TicketPatch ticket);

    ResponseEntity<Ticket> updateTicket(Long id,
                                        TicketInput ticket);
}
