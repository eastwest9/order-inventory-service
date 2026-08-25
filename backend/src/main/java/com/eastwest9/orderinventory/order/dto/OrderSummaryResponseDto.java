package com.eastwest9.orderinventory.order.dto;

import com.eastwest9.orderinventory.order.domain.Order;
import com.eastwest9.orderinventory.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponseDto(
        Long id,
        String orderNumber,
        Long memberId,
        OrderStatus status,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime canceledAt,
        LocalDateTime createdAt
) {

    public static OrderSummaryResponseDto from(Order order) {
        return new OrderSummaryResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getMember().getId(),
                order.getStatus(),
                order.getTotalPrice(),
                order.getItems().size(),
                order.getCanceledAt(),
                order.getCreatedAt()
        );
    }
}
