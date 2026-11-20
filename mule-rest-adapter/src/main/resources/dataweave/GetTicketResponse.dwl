%dw 2.0
ns t http://soa.ifmo.ru/ticket/ws

fun mapGetTicketResponse(payload) = do {
    var body = payload.body
    var item = body.t#getTicketByIdResponse.t#ticket
    ---
    {
        id: item.t#id as Number,
        name: item.t#name as String,
        coordinates: {
            x: item.t#coordinates.t#x as Number,
            y: item.t#coordinates.t#y as Number
        },
        creationDate: item.t#creationDate as String,
        price: item.t#price as Number,
        comment: item.t#comment as String,
        "type": item.t#'type' as String,
        event: if (item.t#event?) {
            id: item.t#event.t#id as Number,
            name: item.t#event.t#name as String,
            ticketsCount: item.t#event.t#ticketsCount as Number,
            eventType: if (item.t#event.t#eventType?)
                item.t#event.t#eventType as String
            else null
        } else null
    }
}
