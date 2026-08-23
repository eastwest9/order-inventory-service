package com.eastwest9.orderinventory.inventory.exception;

public class InvalidInventoryQuantityException extends IllegalArgumentException {

    public InvalidInventoryQuantityException(String message) {
        super(message);
    }

    public InvalidInventoryQuantityException(String message, Throwable cause) {
        super(message, cause);
    }
}
