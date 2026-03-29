package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderStatusUpdateRequest;
import com.grootan.storeflow.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Order Controller", description = "APIs for placing orders, viewing orders, updating order status, generating PDF reports, and exporting CSV data")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Place a new order",
            description = "Creates a new order for the authenticated user",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order placed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order creation request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateOrderRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                     "street": "12 Main Road",
                                               "city": "Coimbatore",
                                               "country": "India",
                                               "postalCode": "641001",
                                               "items": [
                                                 {
                                                   "productId": 1,
                                                   "quantity": 1
                                                 }
                                               ]
                                    }
                                    """)
                    )
            )
            @RequestBody CreateOrderRequest request,
            Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.placeOrder(request, email);
    }

    @Operation(
            summary = "Get orders",
            description = "Returns orders for the authenticated user. Admin can optionally fetch all orders",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping
    public List<OrderDto> getOrders(
            @Parameter(description = "Set true for admin view to fetch all orders", example = "false")
            @RequestParam(defaultValue = "false") boolean admin,
            Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.getOrders(email, admin);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Returns a single order by its ID. Admin can access any order, user can access only their own",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order fetched successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}")
    public OrderDto getById(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Set true for admin access", example = "false")
            @RequestParam(defaultValue = "false") boolean admin,
            Principal principal) {
        String email = (principal != null && principal.getName() != null)
                ? principal.getName()
                : "user@test.com";
        return orderService.getOrderById(id, email, admin);
    }

    @Operation(
            summary = "Update order status",
            description = "Updates the status of an existing order. Admin only",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OrderDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public OrderDto updateStatus(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Order status update request",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderStatusUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": "CONFIRMED"
                                    }
                                    """)
                    )
            )
            @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }

    @Operation(
            summary = "Download order PDF report",
            description = "Generates and downloads a PDF report for a given order",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "PDF report generated successfully",
            content = @Content(
                    mediaType = "application/pdf",
                    schema = @Schema(type = "string", format = "binary")
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/{id}/report")
    public ResponseEntity<ByteArrayResource> downloadOrderReport(
            @Parameter(description = "Order ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Set true for admin access", example = "false")
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

    @Operation(
            summary = "Export orders as CSV",
            description = "Exports orders as CSV with optional date range filters",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV exported successfully",
                    content = @Content(mediaType = "text/csv")),
            @ApiResponse(responseCode = "400", description = "Invalid date filter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportOrders(
            @Parameter(description = "Export orders from this date", example = "2026-03-01")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Export orders up to this date", example = "2026-03-28")
            @RequestParam(required = false) LocalDate to,
            @Parameter(description = "Set true for admin access", example = "false")
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
