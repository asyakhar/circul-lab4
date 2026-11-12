package ru.ifmo.soa.ticket.model;

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


@Schema(description = "Билет")
@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class TicketInput {
    @JsonProperty("name")
    private String name = null;

    @JsonProperty("coordinates")
    private Coordinates coordinates = null;

    @JsonProperty("price")
    private Double price = null;

    @JsonProperty("comment")
    private String comment = null;

    @JsonProperty("type")
    private TicketType type = null;

    @JsonProperty("event")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private EventInput event = null;


    public TicketInput name(String name) {

        this.name = name;
        return this;
    }



    @Schema(example = "Билет в партер", required = true, description = "Название билета. Строка не может быть пустой")

    @NotNull
    @Size(min = 1)
    public String getName() {
        return name;
    }


    public void setName(String name) {

        this.name = name;
    }

    public TicketInput coordinates(Coordinates coordinates) {

        this.coordinates = coordinates;
        return this;
    }



    @Schema(required = true, description = "")

    @Valid
    @NotNull
    public Coordinates getCoordinates() {
        return coordinates;
    }


    public void setCoordinates(Coordinates coordinates) {

        this.coordinates = coordinates;
    }

    public TicketInput price(Double price) {

        this.price = price;
        return this;
    }



    @Schema(example = "2500.5", required = true, description = "Цена билета. Значение должно быть больше 0")

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    public Double getPrice() {
        return price;
    }


    public void setPrice(Double price) {

        this.price = price;
    }

    public TicketInput comment(String comment) {

        this.comment = comment;
        return this;
    }



    @Schema(example = "Место рядом со сценой", required = true, description = "Комментарий к билету. Поле не может быть null")

    @NotNull
    @Size(max = 341)
    public String getComment() {
        return comment;
    }


    public void setComment(String comment) {

        this.comment = comment;
    }

    public TicketInput type(TicketType type) {

        this.type = type;
        return this;
    }



    @Schema(required = true, description = "")

    @Valid
    @NotNull
    public TicketType getType() {
        return type;
    }


    public void setType(TicketType type) {

        this.type = type;
    }

    public TicketInput event(EventInput event) {

        this.event = event;
        return this;
    }



    @Schema(description = "")

    @Valid
    public EventInput getEvent() {
        return event;
    }


    public void setEvent(EventInput event) {
        this.event = event;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TicketInput ticketInput = (TicketInput) o;
        return Objects.equals(this.name, ticketInput.name) &&
                Objects.equals(this.coordinates, ticketInput.coordinates) &&
                Objects.equals(this.price, ticketInput.price) &&
                Objects.equals(this.comment, ticketInput.comment) &&
                Objects.equals(this.type, ticketInput.type) &&
                Objects.equals(this.event, ticketInput.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, coordinates, price, comment, type, event);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TicketInput {\n");

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
