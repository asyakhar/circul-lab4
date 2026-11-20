%dw 2.0
ns t http://soa.ifmo.ru/ticket/ws

fun mapTicketsAboveTypeRequest(attributes) = t#getTicketsWithTypeGreaterThanRequest: {
    t#'type': attributes.queryParams.'type'
}
