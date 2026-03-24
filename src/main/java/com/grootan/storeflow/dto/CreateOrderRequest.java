package com.grootan.storeflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderRequest {
    @NotNull private String street;
    @NotNull private String city;
    @NotNull private String country;
    @NotNull private String postalCode;
    @Valid @NotEmpty private List<CreateOrderItemRequest> items;

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