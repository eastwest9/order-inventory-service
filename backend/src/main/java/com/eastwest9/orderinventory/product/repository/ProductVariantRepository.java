package com.eastwest9.orderinventory.product.repository;

import com.eastwest9.orderinventory.product.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    boolean existsBySkuCode(String skuCode);
}