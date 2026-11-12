package ru.ifmo.soa.booking.model;

import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public class ErrorResponseViolations   {
  private String field = null;  private String message = null;



  @Schema(example = "ticket.price", required = true, description = "Путь поля или имя параметра.")
  @JsonProperty("field")
  @NotNull
  public String getField() {
    return field;
  }
  public void setField(String field) {
    this.field = field;
  }



  @Schema(example = "price должен быть больше 0; получено -10.", required = true, description = "Условие, которое нарушено, и переданное значение.")
  @JsonProperty("message")
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
    return Objects.equals(field, errorResponseViolations.field) &&
        Objects.equals(message, errorResponseViolations.message);
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
