package com.eastwest9.orderinventory.inventory.exception;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(Long variantId) {
        super("재고를 찾을 수 없습니다. variantId=" + variantId);
    }
}
