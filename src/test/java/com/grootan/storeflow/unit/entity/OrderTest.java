package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void shouldAddOrderItemAndSetBidirectionalRelation() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        order.addOrderItem(item);

        assertEquals(1, order.getOrderItems().size());
        assertSame(order, item.getOrder());
    }

    @Test
    void shouldRecalculateTotalAmountCorrectly() {
        Order order = new Order();

        OrderItem item1 = new OrderItem();
        item1.setQuantity(2);
        item1.setUnitPrice(BigDecimal.valueOf(100));

        OrderItem item2 = new OrderItem();
        item2.setQuantity(1);
        item2.setUnitPrice(BigDecimal.valueOf(200));

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        order.recalculateTotalAmount();

        assertEquals(BigDecimal.valueOf(400), order.getTotalAmount());
    }

    @Test
    void shouldReturnZeroWhenNoOrderItemsPresent() {
        Order order = new Order();

        order.recalculateTotalAmount();

        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
    }

    @Test
    void shouldIgnoreItemsWithNullQuantityOrUnitPriceOrBoth() {
        Order order = new Order();

        OrderItem validItem = new OrderItem();
        validItem.setQuantity(2);
        validItem.setUnitPrice(BigDecimal.valueOf(50));

        OrderItem nullQuantityItem = new OrderItem();
        nullQuantityItem.setQuantity(null);
        nullQuantityItem.setUnitPrice(BigDecimal.valueOf(100));

        OrderItem nullUnitPriceItem = new OrderItem();
        nullUnitPriceItem.setQuantity(3);
        nullUnitPriceItem.setUnitPrice(null);

        OrderItem bothNullItem = new OrderItem();
        bothNullItem.setQuantity(null);
        bothNullItem.setUnitPrice(null);

        order.addOrderItem(validItem);
        order.addOrderItem(nullQuantityItem);
        order.addOrderItem(nullUnitPriceItem);
        order.addOrderItem(bothNullItem);

        order.recalculateTotalAmount();

        assertEquals(BigDecimal.valueOf(100), order.getTotalAmount());
    }

    @Test
    void shouldGenerateReferenceNumberWhenReferenceNumberIsNull() {
        Order order = new Order();

        ReflectionTestUtils.invokeMethod(order, "beforeCreate");

        assertNotNull(order.getReferenceNumber());
        assertTrue(order.getReferenceNumber().startsWith("ORD-"));
    }

    @Test
    void shouldGenerateReferenceNumberWhenReferenceNumberIsBlank() {
        Order order = new Order();
        order.setReferenceNumber(" ");

        ReflectionTestUtils.invokeMethod(order, "beforeCreate");

        assertNotNull(order.getReferenceNumber());
        assertTrue(order.getReferenceNumber().startsWith("ORD-"));
    }

    @Test
    void shouldNotOverrideExistingReferenceNumberInBeforeCreate() {
        Order order = new Order();
        order.setReferenceNumber("ORD-CUSTOM123");

        ReflectionTestUtils.invokeMethod(order, "beforeCreate");

        assertEquals("ORD-CUSTOM123", order.getReferenceNumber());
    }

    @Test
    void shouldRecalculateTotalAmountInBeforeUpdate() {
        Order order = new Order();

        OrderItem item = new OrderItem();
        item.setQuantity(3);
        item.setUnitPrice(BigDecimal.valueOf(75));

        order.addOrderItem(item);

        ReflectionTestUtils.invokeMethod(order, "beforeUpdate");

        assertEquals(BigDecimal.valueOf(225), order.getTotalAmount());
    }

    @Test
    void shouldCoverAllGettersAndSetters() {
        Order order = new Order();
        User user = new User();
        ShippingAddress address = new ShippingAddress("Street 1", "Chennai", "India", "600001");
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        order.setId(10L);
        order.setReferenceNumber("ORD-TEST001");
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(address);
        order.setTotalAmount(BigDecimal.valueOf(999.99));
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(updatedAt);

        assertEquals(10L, order.getId());
        assertEquals("ORD-TEST001", order.getReferenceNumber());
        assertSame(user, order.getCustomer());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertSame(address, order.getShippingAddress());
        assertEquals(BigDecimal.valueOf(999.99), order.getTotalAmount());
        assertEquals(createdAt, order.getCreatedAt());
        assertEquals(updatedAt, order.getUpdatedAt());
        assertNotNull(order.getOrderItems());
    }
}