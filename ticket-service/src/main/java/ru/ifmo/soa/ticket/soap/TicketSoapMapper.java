package ru.ifmo.soa.ticket.soap;

import jakarta.xml.bind.JAXBElement;
import org.springframework.stereotype.Component;
import ru.ifmo.soa.ticket.model.Coordinates;
import ru.ifmo.soa.ticket.model.Event;
import ru.ifmo.soa.ticket.model.EventInput;
import ru.ifmo.soa.ticket.model.EventType;
import ru.ifmo.soa.ticket.model.Ticket;
import ru.ifmo.soa.ticket.model.TicketInput;
import ru.ifmo.soa.ticket.model.TicketPatch;
import ru.ifmo.soa.ticket.model.TicketType;
import ru.ifmo.soa.ticket.soap.contract.ObjectFactory;

@Component
public class TicketSoapMapper {

    private final ObjectFactory factory = new ObjectFactory();

    public TicketInput toModel(
            ru.ifmo.soa.ticket.soap.contract.TicketInput source
    ) {
        return new TicketInput()
                .name(source.getName())
                .coordinates(toModel(source.getCoordinates()))
                .price(source.getPrice())
                .comment(source.getComment())
                .type(TicketType.valueOf(source.getType().value()))
                .event(toModel(value(source.getEvent())));
    }

    public TicketPatch toModel(
            ru.ifmo.soa.ticket.soap.contract.TicketPatch source
    ) {
        TicketPatch target = new TicketPatch();
        target.setName(source.getName());
        target.setCoordinates(toModel(source.getCoordinates()));
        target.setPrice(source.getPrice());
        target.setComment(source.getComment());

        if (source.getType() != null) {
            target.setType(TicketType.valueOf(source.getType().value()));
        }

        if (source.getEvent() != null) {
            target.setEvent(toModel(source.getEvent().getValue()));
        }

        return target;
    }

    public ru.ifmo.soa.ticket.soap.contract.Ticket toSoap(Ticket source) {
        ru.ifmo.soa.ticket.soap.contract.Ticket target =
                factory.createTicket();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setCoordinates(toSoap(source.getCoordinates()));
        target.setCreationDate(source.getCreationDate());
        target.setPrice(source.getPrice());
        target.setComment(source.getComment());
        target.setType(
                ru.ifmo.soa.ticket.soap.contract.TicketType.fromValue(
                        source.getType().toString()
                )
        );
        target.setEvent(toSoap(source.getEvent()));
        return target;
    }

    private Coordinates toModel(
            ru.ifmo.soa.ticket.soap.contract.Coordinates source
    ) {
        if (source == null) {
            return null;
        }
        return new Coordinates().x(source.getX()).y(source.getY());
    }

    private EventInput toModel(
            ru.ifmo.soa.ticket.soap.contract.EventInput source
    ) {
        if (source == null) {
            return null;
        }
        EventInput target = new EventInput()
                .name(source.getName())
                .ticketsCount(source.getTicketsCount());
        if (source.getEventType() != null) {
            target.setEventType(EventType.valueOf(source.getEventType().value()));
        }
        return target;
    }

    private ru.ifmo.soa.ticket.soap.contract.Coordinates toSoap(
            Coordinates source
    ) {
        ru.ifmo.soa.ticket.soap.contract.Coordinates target =
                factory.createCoordinates();
        target.setX(source.getX());
        target.setY(source.getY());
        return target;
    }

    private ru.ifmo.soa.ticket.soap.contract.Event toSoap(Event source) {
        if (source == null) {
            return null;
        }
        ru.ifmo.soa.ticket.soap.contract.Event target = factory.createEvent();
        target.setId(source.getId());
        target.setName(source.getName());
        target.setTicketsCount(source.getTicketsCount());
        if (source.getEventType() != null
                && source.getEventType() != EventType.NULL) {
            target.setEventType(
                    ru.ifmo.soa.ticket.soap.contract.EventType.fromValue(
                            source.getEventType().toString()
                    )
            );
        }
        return target;
    }

    private <T> T value(JAXBElement<T> element) {
        return element == null ? null : element.getValue();
    }
}
