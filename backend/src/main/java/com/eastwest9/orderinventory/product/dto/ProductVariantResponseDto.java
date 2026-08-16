package com.eastwest9.orderinventory.product.dto;

import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVariantResponseDto(
        Long id,
        String skuCode,
        String name,
        BigDecimal salePrice,
        ProductVariantStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductVariantResponseDto from(ProductVariant variant) {
        return new ProductVariantResponseDto(
                variant.getId(),
                variant.getSkuCode(),
                variant.getName(),
                variant.getSalePrice(),
                variant.getStatus(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}