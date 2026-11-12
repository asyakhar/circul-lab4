package ru.ifmo.soa.ticket.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ticket.api.TicketsApiDelegate;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;
import ru.ifmo.soa.ticket.model.TicketPatch;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
public class TicketsApiDelegateImpl implements TicketsApiDelegate {

    private final TicketStore store;
    private final TicketQueryService queryService;

    public TicketsApiDelegateImpl(
            TicketStore store,
            TicketQueryService queryService
    ) {
        this.store = store;
        this.queryService = queryService;
    }

    @Override
    public ResponseEntity<Ticket> createTicket(TicketInput input) {
        TicketStore.TicketSnapshot created = store.create(input);
        Ticket ticket = created.ticket();

        URI location = URI.create("/api/v1/tickets/" + ticket.getId());

        return ResponseEntity
                .created(location)
                .body(ticket);
    }

    @Override
    public ResponseEntity<Ticket> getTicketById(
            Long id,
            String ifNoneMatch
    ) {
        TicketStore.TicketSnapshot snapshot = store.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        String etag = store.buildEtag(snapshot);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity
                .ok()
                .eTag(etag)
                .body(snapshot.ticket());
    }

    @Override
    public ResponseEntity<List<Ticket>> getTickets(
            Integer page,
            Integer size,
            List<String> sort,
            List<String> filter
    ) {
        List<Ticket> tickets = store.findAll();

        queryService.filter(tickets, filter);
        queryService.sort(tickets, sort);

        long from = (long) page * size;

        if (from >= tickets.size()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        int fromIndex = (int) from;
        int toIndex = Math.min(fromIndex + size, tickets.size());

        return ResponseEntity.ok(
                tickets.subList(fromIndex, toIndex)
        );
    }

    @Override
    public ResponseEntity<Ticket> updateTicket(
            Long id,
            TicketInput input
    ) {
        TicketStore.TicketSnapshot updated = store.replace(id, input)
                .orElseThrow(() -> new TicketNotFoundException(id));

        return ResponseEntity.ok(updated.ticket());
    }

    @Override
    public ResponseEntity<Ticket> patchTicket(
            Long id,
            TicketPatch patch
    ) {
        boolean emptyPatch =
                patch.getName() == null
                        && patch.getCoordinates() == null
                        && patch.getPrice() == null
                        && patch.getComment() == null
                        && patch.getType() == null
                        && !patch.isEventProvided();

        if (emptyPatch) {
            throw new InvalidQueryException(
                    "ticket",
                    "Необходимо передать хотя бы одно поле."
            );
        }
        TicketStore.TicketSnapshot updated = store.patch(id, patch)
                .orElseThrow(() -> new TicketNotFoundException(id));

        return ResponseEntity.ok(updated.ticket());
    }

    @Override
    public ResponseEntity<Void> deleteTicket(Long id) {
        store.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> optionsTickets() {
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.ALLOW, "GET, POST, OPTIONS")
                .build();
    }
}