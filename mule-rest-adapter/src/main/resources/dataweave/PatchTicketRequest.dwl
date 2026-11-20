%dw 2.0
ns t http://soa.ifmo.ru/ticket/ws
ns xsi http://www.w3.org/2001/XMLSchema-instance

fun eventInput(event) = {
    t#name: event.name,
    t#ticketsCount: event.ticketsCount,
    (t#eventType: event.eventType) if (event.eventType?)
}

fun mapPatchTicketRequest(attributes) = do {
    var ticket = read(attributes.queryParams.ticket, "application/json")
    ---
    t#patchTicketRequest: {
        t#id: attributes.uriParams.id,
        t#ticket: {
            (t#name: ticket.name) if (ticket.name?),
            (t#coordinates: {
                t#x: ticket.coordinates.x,
                t#y: ticket.coordinates.y
            }) if (ticket.coordinates?),
            (t#price: ticket.price) if (ticket.price?),
            (t#comment: ticket.comment) if (ticket.comment?),
            (t#'type': ticket.'type') if (ticket.'type'?),
            (t#event: eventInput(ticket.event))
                if (ticket.event? and ticket.event != null),
            (t#event @(xsi#nil: "true"): "")
                if (ticket.event? and ticket.event == null)
        }
    }
}
