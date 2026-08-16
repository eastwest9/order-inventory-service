package com.eastwest9.orderinventory.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "variant_name", nullable = false, length = 200)
    private String name;

    @Column(
            name = "sale_price",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal salePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_status", nullable = false, length = 20)
    private ProductVariantStatus status;

    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductVariant(
            String skuCode,
            String name,
            BigDecimal salePrice,
            ProductVariantStatus status
    ) {
        validateSkuCode(skuCode);
        validateName(name);
        validateSalePrice(salePrice);
        validateStatus(status);

        this.skuCode = skuCode;
        this.name = name;
        this.salePrice = salePrice;
        this.status = status;
    }

    void assignProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 null일 수 없습니다.");
        }

        this.product = product;
    }

    private void validateSkuCode(String skuCode) {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("SKU 코드는 필수입니다.");
        }

        if (skuCode.length() > 100) {
            throw new IllegalArgumentException("SKU 코드는 100자를 초과할 수 없습니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("SKU명은 필수입니다.");
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException("SKU명은 200자를 초과할 수 없습니다.");
        }
    }

    private void validateSalePrice(BigDecimal salePrice) {
        if (salePrice == null) {
            throw new IllegalArgumentException("판매가격은 필수입니다.");
        }

        if (salePrice.signum() < 0) {
            throw new IllegalArgumentException("판매가격은 0 이상이어야 합니다.");
        }
    }

    private void validateStatus(ProductVariantStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("SKU 상태는 필수입니다.");
        }
    }
}