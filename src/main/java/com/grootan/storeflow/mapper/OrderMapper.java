package com.grootan.storeflow.mapper;

import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderItemDto;
import com.grootan.storeflow.entity.Order;

import java.util.stream.Collectors;

public class OrderMapper {
    public static OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getReferenceNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCustomer().getEmail(),
                order.getOrderItems().stream()
                        .map(item -> new OrderItemDto(
                                item.getId(),
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getUnitPrice(),
                                item.getSubtotal()
                        ))
                        .collect(Collectors.toList())
        );
    }
}