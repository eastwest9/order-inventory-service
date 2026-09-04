package com.eastwest9.orderinventory.inventory.domain;

import com.eastwest9.orderinventory.common.persistence.BaseTimeEntity;
import com.eastwest9.orderinventory.inventory.exception.InsufficientInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InvalidInventoryQuantityException;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Inventory(ProductVariant productVariant, int initialQuantity) {
        validateProductVariant(productVariant);
        validateNonNegativeQuantity(initialQuantity, "초기 재고 수량");

        this.productVariant = productVariant;
        this.quantity = initialQuantity;
    }

    public void receive(int quantity) {
        if (quantity < 1) {
            throw new InvalidInventoryQuantityException(
                    "입고 수량은 1 이상이어야 합니다."
            );
        }

        try {
            this.quantity = Math.addExact(this.quantity, quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidInventoryQuantityException(
                    "입고 후 재고 수량이 허용 범위를 초과합니다.",
                    exception
            );
        }
    }

    public void decrease(int quantity) {
        if (quantity < 1) {
            throw new InvalidInventoryQuantityException(
                    "주문 차감 수량은 1 이상이어야 합니다."
            );
        }

        if (quantity > this.quantity) {
            throw new InsufficientInventoryException(
                    productVariant.getId(),
                    quantity,
                    this.quantity
            );
        }

        this.quantity -= quantity;
    }

    public void restore(int quantity) {
        if (quantity < 1) {
            throw new InvalidInventoryQuantityException(
                    "주문 취소 복구 수량은 1 이상이어야 합니다."
            );
        }

        try {
            this.quantity = Math.addExact(this.quantity, quantity);
        } catch (ArithmeticException exception) {
            throw new InvalidInventoryQuantityException(
                    "주문 취소 복구 후 재고 수량이 허용 범위를 초과합니다.",
                    exception
            );
        }
    }

    public void adjust(int newQuantity) {
        validateNonNegativeQuantity(newQuantity, "조정 재고 수량");

        if (this.quantity == newQuantity) {
            throw new InvalidInventoryQuantityException(
                    "현재 재고와 동일한 수량으로 조정할 수 없습니다."
            );
        }

        this.quantity = newQuantity;
    }

    private void validateProductVariant(ProductVariant productVariant) {
        if (productVariant == null) {
            throw new IllegalArgumentException("상품 SKU는 null일 수 없습니다.");
        }
    }

    private void validateNonNegativeQuantity(int quantity, String fieldName) {
        if (quantity < 0) {
            throw new InvalidInventoryQuantityException(
                    fieldName + "은 0 이상이어야 합니다."
            );
        }
    }
}
