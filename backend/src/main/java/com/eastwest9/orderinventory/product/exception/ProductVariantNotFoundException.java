package com.eastwest9.orderinventory.product.exception;

public class ProductVariantNotFoundException extends RuntimeException {

    public ProductVariantNotFoundException(Long variantId) {
        super("상품 SKU를 찾을 수 없습니다. variantId=" + variantId);
    }
}
