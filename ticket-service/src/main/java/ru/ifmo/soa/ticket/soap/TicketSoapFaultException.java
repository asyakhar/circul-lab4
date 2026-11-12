package ru.ifmo.soa.ticket.soap;

public class TicketSoapFaultException extends RuntimeException {

    private final int status;
    private final String code;
    private final String field;

    public TicketSoapFaultException(
            int status,
            String code,
            String message,
            String field
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.field = field;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}
