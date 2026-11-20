%dw 2.0
ns t http://soa.ifmo.ru/ticket/ws

fun mapMaxTypeRequest() = t#getTicketWithMaxTypeRequest: {}
