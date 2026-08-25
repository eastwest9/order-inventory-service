package com.eastwest9.orderinventory.order.dto;

import com.eastwest9.orderinventory.order.domain.Order;
import com.eastwest9.orderinventory.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String orderNumber,
        Long memberId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime canceledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponseDto> items
) {

    public static OrderResponseDto from(Order order) {
        List<OrderItemResponseDto> items = order.getItems()
                .stream()
                .map(OrderItemResponseDto::from)
                .toList();

        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getMember().getId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getCanceledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
