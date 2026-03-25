package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderStatusUpdateRequest;
import com.grootan.storeflow.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.placeOrder(request, email);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping
    public List<OrderDto> getOrders(@RequestParam(defaultValue = "false") boolean admin, Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.getOrders(email, admin);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}")
    public OrderDto getById(@PathVariable Long id,
                            @RequestParam(defaultValue = "false") boolean admin,
                            Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.getOrderById(id, email, admin);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public OrderDto updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }

    // PDF REPORT
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}/report")
    public ResponseEntity<ByteArrayResource> downloadOrderReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean admin,
            Principal principal
    ) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";

        byte[] pdf = orderService.generateOrderReport(id, email, admin);

        ByteArrayResource resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename("order-" + id + ".pdf")
                                .build()
                                .toString()
                )
                .body(resource);
    }

    // CSV EXPORT
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportOrders(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "false") boolean admin,
            Principal principal
    ) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";

        byte[] csv = orderService.exportOrdersAsCsv(from, to, email, admin);

        ByteArrayResource resource = new ByteArrayResource(csv);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("orders.csv")
                                .build()
                                .toString()
                )
                .body(resource);
    }
}