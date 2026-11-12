package ru.ifmo.soa.ticket.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.ifmo.soa.ticket.model.EventType;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


@Schema(description = "Мероприятие, для которого предназначен билет")
@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class EventInput {
    @JsonProperty("name")

    private String name = null;

    @JsonProperty("ticketsCount")

    private Integer ticketsCount = null;

    @JsonProperty("eventType")

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private EventType eventType = null;


    public EventInput name(String name) {

        this.name = name;
        return this;
    }



    @Schema(example = "Вечер оперы", required = true, description = "Название мероприятия. Строка не может быть пустой")

    @NotNull
    @Size(min = 1)
    public String getName() {
        return name;
    }


    public void setName(String name) {

        this.name = name;
    }

    public EventInput ticketsCount(Integer ticketsCount) {

        this.ticketsCount = ticketsCount;
        return this;
    }



    @Schema(example = "120", required = true, description = "Количество билетов. Значение должно быть больше 0")

    @NotNull
    @Min(1)
    public Integer getTicketsCount() {
        return ticketsCount;
    }


    public void setTicketsCount(Integer ticketsCount) {

        this.ticketsCount = ticketsCount;
    }

    public EventInput eventType(EventType eventType) {

        this.eventType = eventType;
        return this;
    }



    @Schema(description = "")

    @Valid
    public EventType getEventType() {
        return eventType;
    }


    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventInput eventInput = (EventInput) o;
        return Objects.equals(this.name, eventInput.name) &&
                Objects.equals(this.ticketsCount, eventInput.ticketsCount) &&
                Objects.equals(this.eventType, eventInput.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ticketsCount, eventType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class EventInput {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    ticketsCount: ").append(toIndentedString(ticketsCount)).append("\n");
        sb.append("    eventType: ").append(toIndentedString(eventType)).append("\n");
        sb.append("}");
        return sb.toString();
    }


    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
