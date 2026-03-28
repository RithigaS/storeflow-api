package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request payload for creating a new order")
public class CreateOrderRequest {

    @Schema(
            description = "Street address",
            example = "12, Main Road",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Street is required")
    private String street;

    @Schema(
            description = "City name",
            example = "Coimbatore",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "City is required")
    private String city;

    @Schema(
            description = "Country name",
            example = "India",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Country is required")
    private String country;

    @Schema(
            description = "Postal code (6-digit)",
            example = "641001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Postal code is required")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[0-9]{6}$",
            message = "Postal code must be a valid 6-digit code"
    )
    private String postalCode;

    @ArraySchema(
            schema = @Schema(description = "List of order items"),
            minItems = 1
    )
    @Valid
    @NotEmpty(message = "Order items are required")
    private List<CreateOrderItemRequest> items;

    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getPostalCode() { return postalCode; }
    public List<CreateOrderItemRequest> getItems() { return items; }

    public void setStreet(String street) { this.street = street; }
    public void setCity(String city) { this.city = city; }
    public void setCountry(String country) { this.country = country; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setItems(List<CreateOrderItemRequest> items) { this.items = items; }
}