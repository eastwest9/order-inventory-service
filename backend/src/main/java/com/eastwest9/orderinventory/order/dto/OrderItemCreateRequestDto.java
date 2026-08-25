package com.eastwest9.orderinventory.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemCreateRequestDto(
        @NotNull(message = "상품 SKU ID는 필수입니다.")
        Long variantId,

        @NotNull(message = "주문 수량은 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}
