package com.eastwest9.orderinventory.inventory.dto;

import com.eastwest9.orderinventory.inventory.domain.Inventory;

import java.time.LocalDateTime;

public record InventoryResponseDto(
        Long id,
        Long variantId,
        String skuCode,
        int quantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InventoryResponseDto from(Inventory inventory) {
        return new InventoryResponseDto(
                inventory.getId(),
                inventory.getProductVariant().getId(),
                inventory.getProductVariant().getSkuCode(),
                inventory.getQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
