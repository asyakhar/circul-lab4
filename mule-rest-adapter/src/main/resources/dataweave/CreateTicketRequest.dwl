%dw 2.0
ns t http://soa.ifmo.ru/ticket/ws

fun eventInput(event) = {
    t#name: event.name,
    t#ticketsCount: event.ticketsCount,
    (t#eventType: event.eventType) if (event.eventType?)
}

fun mapCreateTicketRequest(attributes) = do {
    var ticket = read(attributes.queryParams.ticket, "application/json")
    ---
    t#createTicketRequest: {
        t#ticket: {
            t#name: ticket.name,
            t#coordinates: {
                t#x: ticket.coordinates.x,
                t#y: ticket.coordinates.y
            },
            t#price: ticket.price,
            t#comment: ticket.comment,
            t#'type': ticket.'type',
            (t#event: eventInput(ticket.event))
                if (ticket.event? and ticket.event != null)
        }
    }
}
