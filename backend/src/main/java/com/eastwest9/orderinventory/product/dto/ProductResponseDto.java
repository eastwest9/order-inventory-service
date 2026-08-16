package com.eastwest9.orderinventory.product.dto;

import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponseDto(
        Long id,
        String name,
        ProductStatus status,
        List<ProductVariantResponseDto> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponseDto from(Product product) {
        List<ProductVariantResponseDto> variants = product.getVariants()
                .stream()
                .map(ProductVariantResponseDto::from)
                .toList();

        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getStatus(),
                variants,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}