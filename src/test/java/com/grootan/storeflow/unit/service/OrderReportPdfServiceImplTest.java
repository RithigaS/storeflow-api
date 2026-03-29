package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.service.impl.OrderReportPdfServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderReportPdfServiceImplTest {

    private final OrderReportPdfServiceImpl orderReportPdfService = new OrderReportPdfServiceImpl();

    @Test
    void generateOrderReportReturnsNonEmptyPdfContainingExpectedOrderReferenceText() throws IOException {
        Order order = buildOrder();

        byte[] pdfBytes = orderReportPdfService.generateOrderReport(order);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            String extractedText = new PDFTextStripper().getText(document);

            assertFalse(extractedText.isBlank());
            assertTrue(extractedText.contains("Order Reference: #" + order.getId()));
            assertTrue(extractedText.contains("Customer Name: " + order.getCustomer().getFullName()));
            assertTrue(extractedText.contains("Order Total: " + order.getTotalAmount()));
        }
    }

    @Test
    void generateOrderReportHandlesNullCreatedAt() throws IOException {
        Order order = buildOrder();
        order.setCreatedAt(null);

        byte[] pdfBytes = orderReportPdfService.generateOrderReport(order);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            String extractedText = new PDFTextStripper().getText(document);

            assertTrue(extractedText.contains("Order Date: N/A"));
        }
    }
    @Test
    void generateOrderReportContainsAllOrderItems() throws IOException {
        Order order = buildOrder();

        Category category = new Category();
        category.setName("Electronics");

        Product extraProduct = new Product();
        extraProduct.setId(202L);
        extraProduct.setName("Mouse");
        extraProduct.setSku("MOU-777");
        extraProduct.setPrice(BigDecimal.valueOf(1500));
        extraProduct.setStockQuantity(5);
        extraProduct.setStatus(ProductStatus.ACTIVE);
        extraProduct.setCategory(category);

        OrderItem extraItem = new OrderItem();
        extraItem.setProduct(extraProduct);
        extraItem.setQuantity(1);
        extraItem.setUnitPrice(extraProduct.getPrice());
        extraItem.calculateSubtotal();

        order.addOrderItem(extraItem);
        order.recalculateTotalAmount();

        byte[] pdfBytes = orderReportPdfService.generateOrderReport(order);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            String extractedText = new PDFTextStripper().getText(document);

            assertTrue(extractedText.contains("Laptop"));
            assertTrue(extractedText.contains("Mouse"));
        }
    }
    private Order buildOrder() {
        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic items");

        Product product = new Product();
        product.setId(101L);
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setSku("LAP-UNIT-001");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setEnabled(true);

        Order order = new Order();
        order.setId(1001L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(new ShippingAddress(
                "Street 1",
                "Coimbatore",
                "India",
                "641001"
        ));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 30));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        return order;
    }
}