package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.dto.CreateOrderItemRequest;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.NotificationPayload;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.exception.InsufficientStockException;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.mapper.OrderMapper;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.EmailService;
import com.grootan.storeflow.service.NotificationService;
import com.grootan.storeflow.service.OrderReportPdfService;
import com.grootan.storeflow.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderReportPdfService orderReportPdfService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private final Counter orderCounter;
    private final Counter revenueCounter;

    private double totalOrderValue = 0;
    private long totalOrders = 0;

    @Value("${app.stock.low-threshold:5}")
    private int lowStockThreshold;

    @Autowired
    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderReportPdfService orderReportPdfService,
            NotificationService notificationService,
            EmailService emailService,
            MeterRegistry meterRegistry
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderReportPdfService = orderReportPdfService;
        this.notificationService = notificationService;
        this.emailService = emailService;

        if (meterRegistry != null) {
            this.orderCounter = meterRegistry.counter("order_placed_total");
            this.revenueCounter = meterRegistry.counter("order_revenue_total");

            Gauge.builder("order_value_avg", this, service -> {
                if (service.totalOrders == 0) return 0;
                return service.totalOrderValue / service.totalOrders;
            }).register(meterRegistry);
        } else {
            this.orderCounter = null;
            this.revenueCounter = null;
        }
    }

    // ✅ Backward compatible constructor (for tests)
    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderReportPdfService orderReportPdfService,
            NotificationService notificationService
    ) {
        this(orderRepository, productRepository, userRepository,
                orderReportPdfService, notificationService,
                null, null);
    }

    @Override
    public OrderDto placeOrder(CreateOrderRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress(
                request.getStreet(),
                request.getCity(),
                request.getCountry(),
                request.getPostalCode()
        ));

        for (CreateOrderItemRequest reqItem : request.getItems()) {
            Product product = productRepository.findById(reqItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            if (product.getStockQuantity() < reqItem.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - reqItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(reqItem.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.calculateSubtotal();
            order.addOrderItem(item);

            if (product.getStockQuantity() < lowStockThreshold) {
                emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());
            }
        }

        order.recalculateTotalAmount();
        Order savedOrder = orderRepository.save(order);

        //  Metrics (safe)
        if (orderCounter != null) {
            orderCounter.increment();
        }

        if (savedOrder.getTotalAmount() != null) {
            double value = savedOrder.getTotalAmount().doubleValue();

            if (revenueCounter != null) {
                revenueCounter.increment(value);
            }

            totalOrderValue += value;
            totalOrders++;
        }

        return OrderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getOrders(String userEmail, boolean isAdmin) {
        List<Order> orders;

        if (isAdmin) {
            orders = orderRepository.findAll();
        } else {
            User user = userRepository.findByEmailIgnoreCase(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            orders = orderRepository.findByCustomer(user);
        }

        return orders.stream()
                .map(OrderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id, String userEmail, boolean isAdmin) {
        Order order = orderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!isAdmin && !order.getCustomer().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Order not found");
        }

        return OrderMapper.toDto(order);
    }

    @Override
    public OrderDto updateStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!isValidTransition(order.getStatus(), status)) {
            throw new InvalidStatusTransitionException("Invalid status transition");
        }

        order.setStatus(status);
        Order saved = orderRepository.save(order);

        NotificationPayload payload = new NotificationPayload(
                "Order status updated",
                status.name(),
                LocalDateTime.now()
        );

        notificationService.sendOrderStatusUpdate(
                saved.getId(),
                saved.getCustomer().getId(),
                payload
        );

        if (status == OrderStatus.CONFIRMED) {
            String orderSummary = buildOrderSummary(saved);
            emailService.sendOrderConfirmationEmail(saved.getCustomer().getEmail(), orderSummary);
        }

        return OrderMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateOrderReport(Long id, String userEmail, boolean isAdmin) {
        Order order = getAccessibleOrder(id, userEmail, isAdmin);
        return orderReportPdfService.generateOrderReport(order);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersAsCsv(LocalDate from, LocalDate to, String userEmail, boolean isAdmin) {
        List<Order> orders;

        if (isAdmin) {
            orders = orderRepository.findAll();
        } else {
            User user = userRepository.findByEmailIgnoreCase(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            orders = orderRepository.findByCustomer(user);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("orderId,orderDate,customerName,customerEmail,status,productName,quantity,unitPrice,subtotal,totalAmount\n");

        for (Order order : orders) {
            LocalDate orderDate = order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null;

            if (from != null && orderDate != null && orderDate.isBefore(from)) continue;
            if (to != null && orderDate != null && orderDate.isAfter(to)) continue;

            for (OrderItem item : order.getOrderItems()) {
                csv.append(order.getId()).append(",");
                csv.append(order.getCreatedAt() != null ? order.getCreatedAt() : "").append(",");
                csv.append(escapeCsv(order.getCustomer().getFullName())).append(",");
                csv.append(escapeCsv(order.getCustomer().getEmail())).append(",");
                csv.append(order.getStatus().name()).append(",");
                csv.append(escapeCsv(item.getProduct().getName())).append(",");
                csv.append(item.getQuantity()).append(",");
                csv.append(item.getUnitPrice()).append(",");
                csv.append(item.getSubtotal()).append(",");
                csv.append(order.getTotalAmount()).append("\n");
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Order getAccessibleOrder(Long id, String userEmail, boolean isAdmin) {
        Order order = orderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!isAdmin && !order.getCustomer().getEmail().equalsIgnoreCase(userEmail)) {
            throw new ResourceNotFoundException("Order not found");
        }

        return order;
    }

    private String buildOrderSummary(Order order) {
        StringBuilder summary = new StringBuilder();
        summary.append("Order ID: ").append(order.getId()).append("\n");

        for (OrderItem item : order.getOrderItems()) {
            summary.append("- ")
                    .append(item.getProduct().getName())
                    .append(" | Qty: ").append(item.getQuantity())
                    .append(" | Price: ").append(formatAmount(item.getUnitPrice()))
                    .append("\n");
        }

        summary.append("Total: ").append(formatAmount(order.getTotalAmount()));
        return summary.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED || next == OrderStatus.CANCELLED;
            case SHIPPED -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}