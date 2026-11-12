package ru.ifmo.soa.ticket.service;

public class InvalidQueryException extends RuntimeException {

    private final String field;

    public InvalidQueryException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}