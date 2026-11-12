package ru.ifmo.soa.ticket.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.ifmo.soa.ticket.api.TicketQueriesApiDelegate;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketType;

import java.util.Comparator;
import java.util.List;

@Service
public class TicketQueriesApiDelegateImpl implements TicketQueriesApiDelegate {

    private final TicketStore store;

    public TicketQueriesApiDelegateImpl(TicketStore store) {
        this.store = store;
    }

    @Override
    public ResponseEntity<Ticket> getTicketWithMaxType() {
        Ticket ticket = store.findAll()
                .stream()
                .max(Comparator.comparingInt(
                        item -> item.getType().ordinal()
                ))
                .orElseThrow(EmptyTicketCollectionException::new);

        return ResponseEntity.ok(ticket);
    }

    @Override
    public ResponseEntity<List<Ticket>> getTicketsByCommentSubstring(
            String substring
    ) {
        List<Ticket> result = store.findAll()
                .stream()
                .filter(ticket ->
                        ticket.getComment().contains(substring)
                )
                .toList();

        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<Ticket>> getTicketsWithTypeGreaterThan(
            TicketType type
    ) {
        List<Ticket> result = store.findAll()
                .stream()
                .filter(ticket ->
                        ticket.getType().ordinal() > type.ordinal()
                )
                .toList();

        return ResponseEntity.ok(result);
    }
}