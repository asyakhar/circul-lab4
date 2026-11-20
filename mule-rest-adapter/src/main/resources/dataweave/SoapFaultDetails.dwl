%dw 2.0
ns soap11 http://schemas.xmlsoap.org/soap/envelope/
ns soap12 http://www.w3.org/2003/05/soap-envelope
ns t http://soa.ifmo.ru/ticket/ws

fun mapSoapFaultDetails(error) = do {
    var rawDetail = error.errorMessage.payload.detail default "<detail/>"
    var document = read(rawDetail, "application/xml")
    var detail = document.detail
        default document.soap11#detail
        default document.soap12#Detail
        default document
    ---
    {
        status: (detail.t#status default 500) as Number,
        code: (detail.t#code default "INTERNAL_ERROR") as String,
        message: (detail.t#message default error.description
        default "Внутренняя ошибка SOAP-сервиса.") as String,
        field: if (detail.t#field?) detail.t#field as String else null
    }
}
