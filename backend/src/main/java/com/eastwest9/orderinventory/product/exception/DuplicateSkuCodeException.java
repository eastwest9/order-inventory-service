package com.eastwest9.orderinventory.product.exception;

public class DuplicateSkuCodeException extends RuntimeException {

    public DuplicateSkuCodeException(String skuCode) {
        super("이미 존재하는 SKU 코드입니다. skuCode=" + skuCode);
    }
}