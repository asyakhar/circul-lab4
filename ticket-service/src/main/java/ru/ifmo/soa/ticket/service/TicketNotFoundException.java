package ru.ifmo.soa.ticket.service;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(long id) {
        super("Билет с id " + id + " не найден.");
    }
}