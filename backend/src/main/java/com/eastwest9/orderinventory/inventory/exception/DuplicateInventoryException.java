package com.eastwest9.orderinventory.inventory.exception;

public class DuplicateInventoryException extends RuntimeException {

    public DuplicateInventoryException(Long variantId) {
        super("이미 재고가 생성된 SKU입니다. variantId=" + variantId);
    }
}
