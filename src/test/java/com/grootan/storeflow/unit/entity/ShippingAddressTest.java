package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.ShippingAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShippingAddressTest {

    @Test
    void shouldCreateShippingAddressUsingConstructor() {
        ShippingAddress address = new ShippingAddress(
                "Street 1",
                "Chennai",
                "India",
                "600001"
        );

        assertEquals("Street 1", address.getStreet());
        assertEquals("Chennai", address.getCity());
        assertEquals("India", address.getCountry());
        assertEquals("600001", address.getPostalCode());
    }

    @Test
    void shouldSetAndGetAllFields() {
        ShippingAddress address = new ShippingAddress();
        address.setStreet("Street 2");
        address.setCity("Coimbatore");
        address.setCountry("India");
        address.setPostalCode("641001");

        assertEquals("Street 2", address.getStreet());
        assertEquals("Coimbatore", address.getCity());
        assertEquals("India", address.getCountry());
        assertEquals("641001", address.getPostalCode());
    }
}