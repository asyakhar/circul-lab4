package ru.ifmo.soa.ticket.service;

import ru.ifmo.soa.ticket.model.TicketPatch;
import org.springframework.stereotype.Repository;
import ru.ifmo.soa.ticket.model.Coordinates;
import ru.ifmo.soa.ticket.model.Event;
import ru.ifmo.soa.ticket.model.EventInput;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TicketStore {

    private static final DateTimeFormatter CREATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Map<Long, StoredTicket> tickets = new LinkedHashMap<>();
    private final Clock clock = Clock.systemUTC();

    private long nextTicketId = 1;
    private long nextEventId = 1;

    public synchronized TicketSnapshot create(TicketInput input) {
        long ticketId = nextTicketId++;

        Ticket ticket = new Ticket()
                .id(ticketId)
                .name(input.getName())
                .coordinates(copyCoordinates(input.getCoordinates()))
                .creationDate(LocalDateTime.now(clock).format(CREATION_DATE_FORMAT))
                .price(input.getPrice())
                .comment(input.getComment())
                .type(input.getType())
                .event(createEvent(input.getEvent()));

        StoredTicket stored = new StoredTicket(ticket, 1);
        tickets.put(ticketId, stored);

        return snapshot(stored);
    }

    public synchronized Optional<TicketSnapshot> findById(long id) {
        StoredTicket stored = tickets.get(id);

        if (stored == null) {
            return Optional.empty();
        }

        return Optional.of(snapshot(stored));
    }

    public synchronized List<Ticket> findAll() {
        List<Ticket> result = new ArrayList<>(tickets.size());

        for (StoredTicket stored : tickets.values()) {
            result.add(copyTicket(stored.ticket()));
        }

        return result;
    }

    public synchronized Optional<TicketSnapshot> replace(
            long id,
            TicketInput input
    ) {
        StoredTicket current = tickets.get(id);

        if (current == null) {
            return Optional.empty();
        }

        Ticket currentTicket = current.ticket();

        Ticket replacement = new Ticket()
                .id(currentTicket.getId())
                .name(input.getName())
                .coordinates(copyCoordinates(input.getCoordinates()))
                .creationDate(currentTicket.getCreationDate())
                .price(input.getPrice())
                .comment(input.getComment())
                .type(input.getType())
                .event(replaceEvent(currentTicket.getEvent(), input.getEvent()));

        StoredTicket updated = new StoredTicket(
                replacement,
                current.version() + 1
        );

        tickets.put(id, updated);

        return Optional.of(snapshot(updated));
    }

    public synchronized Optional<TicketSnapshot> patch(
            long id,
            TicketPatch patch
    ) {
        StoredTicket current = tickets.get(id);

        if (current == null) {
            return Optional.empty();
        }

        Ticket ticket = copyTicket(current.ticket());

        if (patch.getName() != null) {
            ticket.setName(patch.getName());
        }
        if (patch.getCoordinates() != null) {
            ticket.setCoordinates(copyCoordinates(patch.getCoordinates()));
        }
        if (patch.getPrice() != null) {
            ticket.setPrice(patch.getPrice());
        }
        if (patch.getComment() != null) {
            ticket.setComment(patch.getComment());
        }
        if (patch.getType() != null) {
            ticket.setType(patch.getType());
        }
        if (patch.isEventProvided()) {
            if (patch.getEvent() == null) {
                ticket.setEvent(null);
            } else {
                ticket.setEvent(
                        replaceEvent(ticket.getEvent(), patch.getEvent())
                );
            }
        }

        StoredTicket updated = new StoredTicket(
                ticket,
                current.version() + 1
        );

        tickets.put(id, updated);
        return Optional.of(snapshot(updated));
    }

    public synchronized boolean delete(long id) {
        return tickets.remove(id) != null;
    }

    public String buildEtag(TicketSnapshot snapshot) {
        return "\"ticket-"
                + snapshot.ticket().getId()
                + "-v"
                + snapshot.version()
                + "\"";
    }

    private Event replaceEvent(
            Event currentEvent,
            EventInput newEvent
    ) {
        if (newEvent == null) {
            return null;
        }

        if (hasSameEventFields(currentEvent, newEvent)) {
            return new Event()
                    .id(currentEvent.getId())
                    .name(newEvent.getName())
                    .ticketsCount(newEvent.getTicketsCount())
                    .eventType(newEvent.getEventType());
        }

        return createEvent(newEvent);
    }

    private boolean hasSameEventFields(
            Event currentEvent,
            EventInput newEvent
    ) {
        if (currentEvent == null) {
            return false;
        }

        return Objects.equals(currentEvent.getName(), newEvent.getName())
                && Objects.equals(
                currentEvent.getTicketsCount(),
                newEvent.getTicketsCount()
        )
                && Objects.equals(
                currentEvent.getEventType(),
                newEvent.getEventType()
        );
    }

    private Event createEvent(EventInput input) {
        if (input == null) {
            return null;
        }

        return new Event()
                .id(nextEventId++)
                .name(input.getName())
                .ticketsCount(input.getTicketsCount())
                .eventType(input.getEventType());
    }

    private TicketSnapshot snapshot(StoredTicket stored) {
        return new TicketSnapshot(
                copyTicket(stored.ticket()),
                stored.version()
        );
    }

    private Ticket copyTicket(Ticket source) {
        return new Ticket()
                .id(source.getId())
                .name(source.getName())
                .coordinates(copyCoordinates(source.getCoordinates()))
                .creationDate(source.getCreationDate())
                .price(source.getPrice())
                .comment(source.getComment())
                .type(source.getType())
                .event(copyEvent(source.getEvent()));
    }

    private Coordinates copyCoordinates(Coordinates source) {
        if (source == null) {
            return null;
        }

        return new Coordinates()
                .x(source.getX())
                .y(source.getY());
    }

    private Event copyEvent(Event source) {
        if (source == null) {
            return null;
        }

        return new Event()
                .id(source.getId())
                .name(source.getName())
                .ticketsCount(source.getTicketsCount())
                .eventType(source.getEventType());
    }

    private record StoredTicket(
            Ticket ticket,
            long version
    ) {
    }

    public record TicketSnapshot(
            Ticket ticket,
            long version
    ) {
    }
}