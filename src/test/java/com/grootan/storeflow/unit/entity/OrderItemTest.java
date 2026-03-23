package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void shouldCalculateSubtotalWhenQuantityAndUnitPriceArePresent() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(150));

        item.calculateSubtotal();

        assertEquals(BigDecimal.valueOf(300), item.getSubtotal());
    }

    @Test
    void shouldCalculateSubtotalCorrectlyForDecimalValues() {
        OrderItem item = new OrderItem();
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.valueOf(19.99));

        item.calculateSubtotal();

        assertEquals(BigDecimal.valueOf(99.95), item.getSubtotal());
    }

    @Test
    void shouldNotChangeSubtotalWhenUnitPriceIsNull() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        item.setUnitPrice(null);

        item.calculateSubtotal();

        assertEquals(BigDecimal.ZERO, item.getSubtotal());
    }

    @Test
    void shouldNotChangeSubtotalWhenQuantityIsNull() {
        OrderItem item = new OrderItem();
        item.setQuantity(null);
        item.setUnitPrice(BigDecimal.valueOf(99));

        item.calculateSubtotal();

        assertEquals(BigDecimal.ZERO, item.getSubtotal());
    }

    @Test
    void shouldNotChangeSubtotalWhenBothQuantityAndUnitPriceAreNull() {
        OrderItem item = new OrderItem();
        item.setQuantity(null);
        item.setUnitPrice(null);

        item.calculateSubtotal();

        assertEquals(BigDecimal.ZERO, item.getSubtotal());
    }

    @Test
    void shouldCoverAllGettersAndSetters() {
        OrderItem item = new OrderItem();
        Order order = new Order();
        Product product = new Product();

        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(4);
        item.setUnitPrice(BigDecimal.valueOf(25));
        item.setSubtotal(BigDecimal.valueOf(100));

        assertEquals(1L, item.getId());
        assertSame(order, item.getOrder());
        assertSame(product, item.getProduct());
        assertEquals(4, item.getQuantity());
        assertEquals(BigDecimal.valueOf(25), item.getUnitPrice());
        assertEquals(BigDecimal.valueOf(100), item.getSubtotal());
    }
}