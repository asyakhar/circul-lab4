package ru.ifmo.soa.booking.service;

public class TicketServiceCallException extends RuntimeException {

    public TicketServiceCallException(String message) {
        super(message);
    }

    public TicketServiceCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
