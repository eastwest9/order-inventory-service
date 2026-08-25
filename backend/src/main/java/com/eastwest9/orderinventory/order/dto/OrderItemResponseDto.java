package com.eastwest9.orderinventory.order.dto;

import com.eastwest9.orderinventory.order.domain.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponseDto(
        Long id,
        Long variantId,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {

    public static OrderItemResponseDto from(OrderItem orderItem) {
        return new OrderItemResponseDto(
                orderItem.getId(),
                orderItem.getProductVariant().getId(),
                orderItem.getProductName(),
                orderItem.getVariantName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getTotalPrice()
        );
    }
}
