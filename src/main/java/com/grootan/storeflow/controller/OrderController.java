package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderStatusUpdateRequest;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        String email = principal != null ? principal.getName() : "admin@grootan.com";
        return orderService.placeOrder(request, email);
    }

    @GetMapping
    public List<OrderDto> getOrders(@RequestParam(defaultValue = "false") boolean admin, Principal principal) {
        String email = principal != null ? principal.getName() : "admin@grootan.com";
        return orderService.getOrders(email, admin);
    }

    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean admin, Principal principal) {
        String email = principal != null ? principal.getName() : "admin@grootan.com";
        return orderService.getOrderById(id, email, admin);
    }

    @PatchMapping("/{id}/status")
    public OrderDto updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }
}