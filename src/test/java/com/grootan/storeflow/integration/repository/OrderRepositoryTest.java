package com.grootan.storeflow.integration.repository;

import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest extends TestContainerConfig {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private User createUser() {
        User user = new User();
        user.setEmail("orderuser-" + UUID.randomUUID() + "@gmail.com");
        user.setPassword("password123");
        user.setFullName("Order User");
        return userRepository.saveAndFlush(user);
    }

    private Category createCategory() {
        Category category = new Category();
        category.setName("Books-" + UUID.randomUUID());
        category.setDescription("Books category");
        return categoryRepository.saveAndFlush(category);
    }

    private Product createProduct(Category category, BigDecimal price) {
        Product product = new Product();
        product.setName("Book-" + UUID.randomUUID());
        product.setDescription("A nice book");
        product.setSku("BOOK-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(price);
        product.setStockQuantity(10);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }

    @Test
    void shouldSaveOrderWithItemsUsingCascade() {
        User user = createUser();
        Category category = createCategory();
        Product product = createProduct(category, BigDecimal.valueOf(50));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress("Street 1", "Chennai", "India", "600001"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(BigDecimal.valueOf(50));

        order.addOrderItem(item);

        Order savedOrder = orderRepository.saveAndFlush(order);

        assertNotNull(savedOrder.getId());
        assertEquals(1, savedOrder.getOrderItems().size());
    }

    @Test
    void shouldCalculateTotalAmountCorrectly() {
        User user = createUser();
        Category category = createCategory();
        Product product1 = createProduct(category, BigDecimal.valueOf(100));
        Product product2 = createProduct(category, BigDecimal.valueOf(200));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress("Street 2", "Coimbatore", "India", "641001"));

        OrderItem item1 = new OrderItem();
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(BigDecimal.valueOf(100));

        OrderItem item2 = new OrderItem();
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(BigDecimal.valueOf(200));

        order.addOrderItem(item1);
        order.addOrderItem(item2);

        Order savedOrder = orderRepository.saveAndFlush(order);

        assertEquals(BigDecimal.valueOf(400), savedOrder.getTotalAmount());
    }

    @Test
    void shouldGenerateReferenceNumberOnSave() {
        User user = createUser();
        Category category = createCategory();
        Product product = createProduct(category, BigDecimal.valueOf(120));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress("Street 3", "Madurai", "India", "625001"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(120));

        order.addOrderItem(item);

        Order savedOrder = orderRepository.saveAndFlush(order);

        assertNotNull(savedOrder.getReferenceNumber());
        assertTrue(savedOrder.getReferenceNumber().startsWith("ORD-"));
    }

    @Test
    void shouldFindOrdersByCustomer() {
        User user = createUser();
        Category category = createCategory();
        Product product = createProduct(category, BigDecimal.valueOf(150));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress("Street 4", "Salem", "India", "636001"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.valueOf(150));

        order.addOrderItem(item);
        orderRepository.saveAndFlush(order);

        List<Order> orders = orderRepository.findByCustomer(user);

        assertEquals(1, orders.size());
    }
}