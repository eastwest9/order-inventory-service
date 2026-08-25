package com.eastwest9.orderinventory.product.exception;

public class ProductVariantNotOrderableException extends RuntimeException {

    public ProductVariantNotOrderableException(Long variantId) {
        super("판매 중인 상품과 SKU만 주문할 수 있습니다. variantId=" + variantId);
    }
}
