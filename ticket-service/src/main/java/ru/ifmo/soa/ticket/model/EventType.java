package ru.ifmo.soa.ticket.model;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonCreator;


public enum EventType {
    CONCERT("CONCERT"),
    BASKETBALL("BASKETBALL"),
    OPERA("OPERA"),
    NULL(null);

    private String value;

    EventType(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static EventType fromValue(String text) {
        for (EventType b : EventType.values()) {
            if (b.value != null && b.value.equals(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException(
                "Неизвестный тип мероприятия: " + text
        );
    }
}
