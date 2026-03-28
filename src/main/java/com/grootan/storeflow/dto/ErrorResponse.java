package com.grootan.storeflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response returned by the API")
public class ErrorResponse {

    @Schema(
            description = "Timestamp when the error occurred",
            example = "2026-03-28T14:30:45"
    )
    private String timestamp;

    @Schema(
            description = "HTTP status code",
            example = "400"
    )
    private int status;

    @Schema(
            description = "HTTP error name",
            example = "Bad Request"
    )
    private String error;

    @Schema(
            description = "Main error message",
            example = "Validation failed"
    )
    private String message;

    @Schema(
            description = "API path where the error occurred",
            example = "/api/products"
    )
    private String path;

    @Schema(
            description = "Field-level validation errors",
            example = "{\"name\":\"Product name is required\",\"price\":\"Price must be a positive value\"}"
    )
    private Map<String, String> errors;

    public ErrorResponse() {
    }

    public ErrorResponse(String timestamp, int status, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(String timestamp, int status, String error,
                         String message, String path, Map<String, String> errors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Map<String, String> getErrors() { return errors; }

    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setStatus(int status) { this.status = status; }
    public void setError(String error) { this.error = error; }
    public void setMessage(String message) { this.message = message; }
    public void setPath(String path) { this.path = path; }
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}