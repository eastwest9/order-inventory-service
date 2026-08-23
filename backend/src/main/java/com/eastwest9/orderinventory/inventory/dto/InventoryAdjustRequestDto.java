package com.eastwest9.orderinventory.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryAdjustRequestDto(
        @NotNull(message = "조정 재고 수량은 필수입니다.")
        @Min(value = 0, message = "조정 재고 수량은 0 이상이어야 합니다.")
        Integer quantity
) {
}
