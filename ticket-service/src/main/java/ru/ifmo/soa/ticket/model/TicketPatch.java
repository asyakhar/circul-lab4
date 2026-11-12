package ru.ifmo.soa.ticket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.ifmo.soa.ticket.model.Coordinates;
import ru.ifmo.soa.ticket.model.EventInput;
import ru.ifmo.soa.ticket.model.TicketType;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


@Schema(description = "Поля билета, которые требуется изменить")
@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class TicketPatch {
    @JsonProperty("name")

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private String name = null;

    @JsonProperty("coordinates")

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private Coordinates coordinates = null;

    @JsonProperty("price")

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private Double price = null;

    @JsonProperty("comment")

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private String comment = null;

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private TicketType type = null;

    @JsonProperty("event")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private EventInput event = null;

    @JsonIgnore
    private boolean eventProvided = false;

    public TicketPatch name(String name) {

        this.name = name;
        return this;
    }


    @Schema(example = "Билет в амфитеатр", description = "Новое название билета. Строка не может быть пустой")

    @Size(min = 1)
    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public TicketPatch coordinates(Coordinates coordinates) {

        this.coordinates = coordinates;
        return this;
    }


    @Schema(description = "")

    @Valid
    public Coordinates getCoordinates() {
        return coordinates;
    }


    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public TicketPatch price(Double price) {

        this.price = price;
        return this;
    }


    @Schema(example = "3000", description = "Новая цена билета. Значение должно быть больше 0")

    @DecimalMin(value = "0", inclusive = false)
    public Double getPrice() {
        return price;
    }


    public void setPrice(Double price) {
        this.price = price;
    }

    public TicketPatch comment(String comment) {

        this.comment = comment;
        return this;
    }


    @Schema(example = "Новое место", description = "Новый комментарий. Длина не должна превышать 341 символ")

    @Size(max = 341)
    public String getComment() {
        return comment;
    }


    public void setComment(String comment) {
        this.comment = comment;
    }

    public TicketPatch type(TicketType type) {

        this.type = type;
        return this;
    }


    @Schema(description = "")

    @Valid
    public TicketType getType() {
        return type;
    }


    public void setType(TicketType type) {
        this.type = type;
    }

    public TicketPatch event(EventInput event) {
        this.event = event;
        this.eventProvided = true;
        return this;
    }


    @Schema(description = "")

    @Valid
    public EventInput getEvent() {
        return event;
    }


    @JsonSetter("event")
    public void setEvent(EventInput event) {
        this.event = event;
        this.eventProvided = true;
    }

    @JsonIgnore
    public boolean isEventProvided() {
        return eventProvided;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TicketPatch ticketPatch = (TicketPatch) o;
        return Objects.equals(this.name, ticketPatch.name) &&
                Objects.equals(this.coordinates, ticketPatch.coordinates) &&
                Objects.equals(this.price, ticketPatch.price) &&
                Objects.equals(this.comment, ticketPatch.comment) &&
                Objects.equals(this.type, ticketPatch.type) &&
                Objects.equals(this.event, ticketPatch.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, coordinates, price, comment, type, event);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TicketPatch {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    coordinates: ").append(toIndentedString(coordinates)).append("\n");
        sb.append("    price: ").append(toIndentedString(price)).append("\n");
        sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    event: ").append(toIndentedString(event)).append("\n");
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
