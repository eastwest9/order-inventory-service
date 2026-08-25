package com.eastwest9.orderinventory.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. orderId=" + orderId);
    }
}
