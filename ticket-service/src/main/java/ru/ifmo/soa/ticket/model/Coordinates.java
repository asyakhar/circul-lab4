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

@Schema(description = "Координаты билета")
@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class Coordinates {
    @JsonProperty("x")

    private Double x = null;

    @JsonProperty("y")

    private Long y = null;

    public Coordinates x(Double x) {
        this.x = x;
        return this;
    }

    @Schema(example = "10.5", required = true, description = "Координата X (> -239)")

    @NotNull
    @DecimalMin(value = "-239", inclusive = false)
    public Double getX() {
        return x;
    }


    public void setX(Double x) {
        this.x = x;
    }

    public Coordinates y(Long y) {
        this.y = y;
        return this;
    }

    @Schema(example = "20", required = true, description = "Координата Y (> -495)")

    @NotNull
    @Min(-494L)
    public Long getY() {
        return y;
    }


    public void setY(Long y) {
        this.y = y;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coordinates coordinates = (Coordinates) o;
        return Objects.equals(this.x, coordinates.x) &&
                Objects.equals(this.y, coordinates.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Coordinates {\n");
        sb.append("    x: ").append(toIndentedString(x)).append("\n");
        sb.append("    y: ").append(toIndentedString(y)).append("\n");
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
