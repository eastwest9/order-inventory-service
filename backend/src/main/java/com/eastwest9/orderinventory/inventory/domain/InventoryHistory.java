package com.eastwest9.orderinventory.inventory.domain;

import com.eastwest9.orderinventory.common.persistence.BaseCreatedEntity;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
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
@Entity
@Table(name = "inventory_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistory extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private InventoryChangeType changeType;

    @Column(name = "change_quantity", nullable = false)
    private int changeQuantity;

    @Column(name = "before_quantity", nullable = false)
    private int beforeQuantity;

    @Column(name = "after_quantity", nullable = false)
    private int afterQuantity;

    private InventoryHistory(
            ProductVariant productVariant,
            Long orderItemId,
            InventoryChangeType changeType,
            int beforeQuantity,
            int afterQuantity
    ) {
        validateProductVariant(productVariant);
        validateOrderItemId(orderItemId, changeType);
        validateNonNegativeQuantity(beforeQuantity, "변경 전 재고 수량");
        validateNonNegativeQuantity(afterQuantity, "변경 후 재고 수량");

        int changeQuantity = afterQuantity - beforeQuantity;
        validateChangeQuantity(changeType, changeQuantity);

        this.productVariant = productVariant;
        this.orderItemId = orderItemId;
        this.changeType = changeType;
        this.changeQuantity = changeQuantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
    }

    public static InventoryHistory initial(
            ProductVariant productVariant,
            int initialQuantity
    ) {
        return new InventoryHistory(
                productVariant,
                null,
                InventoryChangeType.INITIAL,
                0,
                initialQuantity
        );
    }

    public static InventoryHistory receipt(
            ProductVariant productVariant,
            int beforeQuantity,
            int afterQuantity
    ) {
        return new InventoryHistory(
                productVariant,
                null,
                InventoryChangeType.RECEIPT,
                beforeQuantity,
                afterQuantity
        );
    }

    public static InventoryHistory adjustment(
            ProductVariant productVariant,
            int beforeQuantity,
            int afterQuantity
    ) {
        return new InventoryHistory(
                productVariant,
                null,
                InventoryChangeType.ADJUSTMENT,
                beforeQuantity,
                afterQuantity
        );
    }

    public static InventoryHistory order(
            ProductVariant productVariant,
            Long orderItemId,
            int beforeQuantity,
            int afterQuantity
    ) {
        return new InventoryHistory(
                productVariant,
                orderItemId,
                InventoryChangeType.ORDER,
                beforeQuantity,
                afterQuantity
        );
    }

    public static InventoryHistory orderCancel(
            ProductVariant productVariant,
            Long orderItemId,
            int beforeQuantity,
            int afterQuantity
    ) {
        return new InventoryHistory(
                productVariant,
                orderItemId,
                InventoryChangeType.ORDER_CANCEL,
                beforeQuantity,
                afterQuantity
        );
    }

    private void validateOrderItemId(
            Long orderItemId,
            InventoryChangeType changeType
    ) {
        boolean orderChange = changeType == InventoryChangeType.ORDER
                || changeType == InventoryChangeType.ORDER_CANCEL;

        if (orderChange && orderItemId == null) {
            throw new IllegalArgumentException("주문 재고 이력의 주문 항목 ID는 필수입니다.");
        }

        if (!orderChange && orderItemId != null) {
            throw new IllegalArgumentException("비주문 재고 이력에는 주문 항목 ID를 지정할 수 없습니다.");
        }
    }

    private void validateProductVariant(ProductVariant productVariant) {
        if (productVariant == null) {
            throw new IllegalArgumentException("상품 SKU는 null일 수 없습니다.");
        }
    }

    private void validateNonNegativeQuantity(int quantity, String fieldName) {
        if (quantity < 0) {
            throw new IllegalArgumentException(fieldName + "은 0 이상이어야 합니다.");
        }
    }

    private void validateChangeQuantity(
            InventoryChangeType changeType,
            int changeQuantity
    ) {
        if (changeType == InventoryChangeType.RECEIPT && changeQuantity < 1) {
            throw new IllegalArgumentException("입고 재고 변경 수량은 1 이상이어야 합니다.");
        }

        if (changeType == InventoryChangeType.ORDER && changeQuantity >= 0) {
            throw new IllegalArgumentException("주문 재고 변경 수량은 음수여야 합니다.");
        }

        if (changeType == InventoryChangeType.ORDER_CANCEL && changeQuantity < 1) {
            throw new IllegalArgumentException("주문 취소 재고 변경 수량은 양수여야 합니다.");
        }

        if (changeType == InventoryChangeType.ADJUSTMENT && changeQuantity == 0) {
            throw new IllegalArgumentException("재고 조정 수량은 0일 수 없습니다.");
        }
    }
}
