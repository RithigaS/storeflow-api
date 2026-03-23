package com.grootan.storeflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShippingAddress {

    @Column(name = "shipping_street", nullable = false)
    private String street;

    @Column(name = "shipping_city", nullable = false)
    private String city;

    @Column(name = "shipping_country", nullable = false)
    private String country;

    @Column(name = "shipping_postal_code", nullable = false)
    private String postalCode;

    public ShippingAddress() {
    }

    public ShippingAddress(String street, String city, String country, String postalCode) {
        this.street = street;
        this.city = city;
        this.country = country;
        this.postalCode = postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}