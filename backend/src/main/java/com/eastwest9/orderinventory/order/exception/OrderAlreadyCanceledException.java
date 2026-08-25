package com.eastwest9.orderinventory.order.exception;

public class OrderAlreadyCanceledException extends IllegalStateException {

    public OrderAlreadyCanceledException() {
        super("이미 취소된 주문입니다.");
    }
}
