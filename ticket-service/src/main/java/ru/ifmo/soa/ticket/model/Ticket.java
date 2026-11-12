package ru.ifmo.soa.ticket.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.ifmo.soa.ticket.model.Coordinates;
import ru.ifmo.soa.ticket.model.Event;
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


public class Ticket {
    @JsonProperty("id")
    private Long id = null;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("coordinates")
    private Coordinates coordinates = null;

    @JsonProperty("creationDate")
    private String creationDate = null;

    @JsonProperty("price")
    private Double price = null;

    @JsonProperty("comment")
    private String comment = null;

    @JsonProperty("type")
    private TicketType type = null;

    @JsonProperty("event")
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonSetter(nulls = Nulls.FAIL)
    private Event event = null;


    public Ticket id(Long id) {

        this.id = id;
        return this;
    }



    @Schema(example = "10", required = true, accessMode = Schema.AccessMode.READ_ONLY, description = "Уникальный автоматически генерируемый идентификатор билета")
    @NotNull
    @Min(1L)
    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }

    public Ticket name(String name) {

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

    public Ticket coordinates(Coordinates coordinates) {

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

    public Ticket creationDate(String creationDate) {

        this.creationDate = creationDate;
        return this;
    }



    @Schema(example = "21.09.2026 14:30:00", required = true, accessMode = Schema.AccessMode.READ_ONLY, description = "Дата и время создания, генерируются сервером. Формат dd.MM.yyyy HH:mm:ss с необязательными долями секунды (1–9 цифр), без часового пояса. Пример: 21 сентября 2026, 14:30. Календарную корректность проверяет сервер.")

    @NotNull
    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.\\d{4} ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.\\d{1,9})?$")
    public String getCreationDate() {
        return creationDate;
    }


    public void setCreationDate(String creationDate) {

        this.creationDate = creationDate;
    }

    public Ticket price(Double price) {

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

    public Ticket comment(String comment) {

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

    public Ticket type(TicketType type) {

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

    public Ticket event(Event event) {

        this.event = event;
        return this;
    }



    @Schema(description = "")

    @Valid
    public Event getEvent() {
        return event;
    }


    public void setEvent(Event event) {
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
        Ticket ticket = (Ticket) o;
        return Objects.equals(this.id, ticket.id) &&
                Objects.equals(this.name, ticket.name) &&
                Objects.equals(this.coordinates, ticket.coordinates) &&
                Objects.equals(this.creationDate, ticket.creationDate) &&
                Objects.equals(this.price, ticket.price) &&
                Objects.equals(this.comment, ticket.comment) &&
                Objects.equals(this.type, ticket.type) &&
                Objects.equals(this.event, ticket.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, coordinates, creationDate, price, comment, type, event);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Ticket {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    coordinates: ").append(toIndentedString(coordinates)).append("\n");
        sb.append("    creationDate: ").append(toIndentedString(creationDate)).append("\n");
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
