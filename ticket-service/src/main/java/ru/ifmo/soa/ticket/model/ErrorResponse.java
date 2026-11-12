package ru.ifmo.soa.ticket.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import ru.ifmo.soa.ticket.model.ErrorResponseViolations;
import org.springframework.validation.annotation.Validated;
import org.openapitools.jackson.nullable.JsonNullable;
import io.swagger.configuration.NotUndefined;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


@Schema(description = "Информация об ошибке выполнения запроса")
@Validated
@NotUndefined
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-09-29T15:09:57.766484+03:00[Europe/Moscow]")


public class ErrorResponse   {
  @JsonProperty("timestamp")

  private String timestamp = null;

  @JsonProperty("status")

  private Integer status = null;

  @JsonProperty("error")

  private String error = null;

  @JsonProperty("message")

  private String message = null;

  @JsonProperty("path")

  private String path = null;

  @JsonProperty("violations")
  @Valid
  private List<ErrorResponseViolations> violations = null;

  public ErrorResponse timestamp(String timestamp) {

    this.timestamp = timestamp;
    return this;
  }



  @Schema(example = "21.09.2026 14:30:00", required = true, description = "Время ошибки в UTC в формате dd.MM.yyyy HH:mm:ss, необязательные доли секунды.")

  @NotNull
@Pattern(regexp="^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.\\d{4} ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.\\d{1,9})?$")   public String getTimestamp() {
    return timestamp;
  }



  public void setTimestamp(String timestamp) {

    this.timestamp = timestamp;
  }

  public ErrorResponse status(Integer status) {

    this.status = status;
    return this;
  }



  @Schema(example = "422", required = true, description = "HTTP-код ошибки")

  @NotNull
@Min(400) @Max(599)   public Integer getStatus() {
    return status;
  }



  public void setStatus(Integer status) {

    this.status = status;
  }

  public ErrorResponse error(String error) {

    this.error = error;
    return this;
  }



  @Schema(example = "Unprocessable Content", required = true, description = "Краткое название ошибки")

  @NotNull
  public String getError() {
    return error;
  }



  public void setError(String error) {

    this.error = error;
  }

  public ErrorResponse message(String message) {

    this.message = message;
    return this;
  }



  @Schema(example = "price должен быть больше 0; получено -10.", required = true, description = "Подробное описание причины ошибки")

  @NotNull
  public String getMessage() {
    return message;
  }



  public void setMessage(String message) {

    this.message = message;
  }

  public ErrorResponse path(String path) {

    this.path = path;
    return this;
  }



  @Schema(example = "/api/v1/tickets", required = true, description = "URL запроса, при обработке которого произошла ошибка")

  @NotNull
  public String getPath() {
    return path;
  }



  public void setPath(String path) {

    this.path = path;
  }

  public ErrorResponse violations(List<ErrorResponseViolations> violations) {

    this.violations = violations;
    return this;
  }

  public ErrorResponse addViolationsItem(ErrorResponseViolations violationsItem) {
    if (this.violations == null) {
      this.violations = new ArrayList<>();
    }
    this.violations.add(violationsItem);
    return this;
  }



  @Schema(description = "Список нарушений схемы с именами полей. Для ошибок разбора может указываться весь параметр ticket.")
  @Valid
  public List<ErrorResponseViolations> getViolations() {
    return violations;
  }



  public void setViolations(List<ErrorResponseViolations> violations) {
    this.violations = violations;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ErrorResponse errorResponse = (ErrorResponse) o;
    return Objects.equals(this.timestamp, errorResponse.timestamp) &&
        Objects.equals(this.status, errorResponse.status) &&
        Objects.equals(this.error, errorResponse.error) &&
        Objects.equals(this.message, errorResponse.message) &&
        Objects.equals(this.path, errorResponse.path) &&
        Objects.equals(this.violations, errorResponse.violations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, status, error, message, path, violations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ErrorResponse {\n");

    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    violations: ").append(toIndentedString(violations)).append("\n");
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
