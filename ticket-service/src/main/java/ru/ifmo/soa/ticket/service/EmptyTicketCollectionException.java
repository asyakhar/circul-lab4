package ru.ifmo.soa.ticket.service;

public class EmptyTicketCollectionException extends RuntimeException {

    public EmptyTicketCollectionException() {
        super("Коллекция билетов пуста.");
    }
}