package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.dto.CreateOrderItemRequest;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.exception.InsufficientStockException;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.mapper.OrderMapper;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OrderDto placeOrder(CreateOrderRequest request, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setCustomer(user);
        order.setShippingAddress(new ShippingAddress(
                request.getStreet(), request.getCity(), request.getCountry(), request.getPostalCode()
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
        }

        order.recalculateTotalAmount();
        return OrderMapper.toDto(orderRepository.save(order));
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
        return orders.stream().map(OrderMapper::toDto).toList();
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
        return OrderMapper.toDto(orderRepository.save(order));
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