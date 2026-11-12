package ru.ifmo.soa.booking.model;

import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import ru.ifmo.soa.booking.model.ErrorResponseViolations;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description="Информация об ошибке выполнения запроса")
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaResteasyServerCodegen", date = "2026-09-29T15:09:58.593749+03:00[Europe/Moscow]")
public class ErrorResponse   {
  private String timestamp = null;  private Integer status = null;  private String error = null;  private String message = null;  private String path = null;  private List<ErrorResponseViolations> violations = new ArrayList<ErrorResponseViolations>();



  @Schema(example = "21.09.2026 14:30:00", required = true, description = "Время ошибки в UTC в формате dd.MM.yyyy HH:mm:ss, необязательные доли секунды.")
  @JsonProperty("timestamp")
  @NotNull
 @Pattern(regexp="^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.\\d{4} ([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](\\.\\d{1,9})?$")  public String getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }



  @Schema(example = "422", required = true, description = "HTTP-код ошибки")
  @JsonProperty("status")
  @NotNull
 @Min(400) @Max(599)  public Integer getStatus() {
    return status;
  }
  public void setStatus(Integer status) {
    this.status = status;
  }



  @Schema(example = "Unprocessable Content", required = true, description = "Краткое название ошибки")
  @JsonProperty("error")
  @NotNull
  public String getError() {
    return error;
  }
  public void setError(String error) {
    this.error = error;
  }



  @Schema(example = "price должен быть больше 0; получено -10.", required = true, description = "Подробное описание причины ошибки")
  @JsonProperty("message")
  @NotNull
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }



  @Schema(example = "/api/v1/tickets", required = true, description = "URL запроса, при обработке которого произошла ошибка")
  @JsonProperty("path")
  @NotNull
  public String getPath() {
    return path;
  }
  public void setPath(String path) {
    this.path = path;
  }



  @Schema(description = "Список нарушений схемы с именами полей. Для ошибок разбора может указываться весь параметр ticket.")
  @JsonProperty("violations")
  @NotNull
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
    return Objects.equals(timestamp, errorResponse.timestamp) &&
        Objects.equals(status, errorResponse.status) &&
        Objects.equals(error, errorResponse.error) &&
        Objects.equals(message, errorResponse.message) &&
        Objects.equals(path, errorResponse.path) &&
        Objects.equals(violations, errorResponse.violations);
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
