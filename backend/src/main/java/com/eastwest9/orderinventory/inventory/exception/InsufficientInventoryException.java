package com.eastwest9.orderinventory.inventory.exception;

import lombok.Getter;

@Getter
public class InsufficientInventoryException extends RuntimeException {

    private final Long variantId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(Long variantId, int requestedQuantity, int availableQuantity) {
        super("재고가 부족합니다. variantId=" + variantId
                + ", requestedQuantity=" + requestedQuantity
                + ", availableQuantity=" + availableQuantity);
        this.variantId = variantId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
}
