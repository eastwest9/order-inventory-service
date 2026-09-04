package com.eastwest9.orderinventory.order.controller;

import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import com.eastwest9.orderinventory.order.dto.OrderSummaryResponseDto;
import com.eastwest9.orderinventory.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderCreateRequestDto request) {
        OrderResponseDto response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long orderId) {
        OrderResponseDto response = orderService.getOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponseDto>> getOrders() {
        List<OrderSummaryResponseDto> response = orderService.getOrders();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId) {
        OrderResponseDto response = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(response);
    }
}
