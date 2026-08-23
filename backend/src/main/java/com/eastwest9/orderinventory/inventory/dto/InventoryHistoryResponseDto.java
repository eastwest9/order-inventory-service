package com.eastwest9.orderinventory.inventory.dto;

import com.eastwest9.orderinventory.inventory.domain.InventoryChangeType;
import com.eastwest9.orderinventory.inventory.domain.InventoryHistory;

import java.time.LocalDateTime;

public record InventoryHistoryResponseDto(
        Long id,
        Long variantId,
        InventoryChangeType changeType,
        int changeQuantity,
        int beforeQuantity,
        int afterQuantity,
        LocalDateTime createdAt
) {

    public static InventoryHistoryResponseDto from(InventoryHistory history) {
        return new InventoryHistoryResponseDto(
                history.getId(),
                history.getProductVariant().getId(),
                history.getChangeType(),
                history.getChangeQuantity(),
                history.getBeforeQuantity(),
                history.getAfterQuantity(),
                history.getCreatedAt()
        );
    }
}
