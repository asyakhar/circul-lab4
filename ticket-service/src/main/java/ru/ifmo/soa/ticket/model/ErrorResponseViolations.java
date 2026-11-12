package ru.ifmo.soa.ticket.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class ErrorResponseViolations {
    @JsonProperty("field")

    private String field = null;

    @JsonProperty("message")

    private String message = null;

    public ErrorResponseViolations field(String field) {

        this.field = field;
        return this;
    }


    @Schema(example = "ticket.price", required = true, description = "Путь поля или имя параметра.")
    @NotNull
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public ErrorResponseViolations message(String message) {

        this.message = message;
        return this;
    }

    @Schema(example = "price должен быть больше 0; получено -10.", required = true, description = "Условие, которое нарушено, и переданное значение.")
    @NotNull
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorResponseViolations errorResponseViolations = (ErrorResponseViolations) o;
        return Objects.equals(this.field, errorResponseViolations.field) &&
                Objects.equals(this.message, errorResponseViolations.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorResponseViolations {\n");

        sb.append("    field: ").append(toIndentedString(field)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
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
