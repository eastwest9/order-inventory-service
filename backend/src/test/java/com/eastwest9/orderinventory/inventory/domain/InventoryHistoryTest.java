package com.eastwest9.orderinventory.inventory.domain;

import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryHistoryTest {

    @Test
    void 초기_재고_이력을_생성한다() {
        ProductVariant productVariant = createProductVariant();

        InventoryHistory history = InventoryHistory.initial(productVariant, 10);

        assertThat(history.getProductVariant()).isEqualTo(productVariant);
        assertThat(history.getOrderItemId()).isNull();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.INITIAL);
        assertThat(history.getChangeQuantity()).isEqualTo(10);
        assertThat(history.getBeforeQuantity()).isZero();
        assertThat(history.getAfterQuantity()).isEqualTo(10);
    }

    @Test
    void 초기_재고_이력의_수량은_음수일_수_없다() {
        assertThatThrownBy(
                () -> InventoryHistory.initial(createProductVariant(), -1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("변경 후 재고 수량은 0 이상이어야 합니다.");
    }

    @Test
    void 입고_이력을_생성한다() {
        InventoryHistory history = InventoryHistory.receipt(
                createProductVariant(),
                10,
                15
        );

        assertThat(history.getOrderItemId()).isNull();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.RECEIPT);
        assertThat(history.getChangeQuantity()).isEqualTo(5);
        assertThat(history.getBeforeQuantity()).isEqualTo(10);
        assertThat(history.getAfterQuantity()).isEqualTo(15);
    }

    @Test
    void 입고_후_수량은_입고_전보다_커야_한다() {
        assertThatThrownBy(
                () -> InventoryHistory.receipt(createProductVariant(), 10, 10)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입고 재고 변경 수량은 1 이상이어야 합니다.");
    }

    @Test
    void 증가하는_재고_조정_이력을_생성한다() {
        InventoryHistory history = InventoryHistory.adjustment(
                createProductVariant(),
                10,
                15
        );

        assertThat(history.getOrderItemId()).isNull();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.ADJUSTMENT);
        assertThat(history.getChangeQuantity()).isEqualTo(5);
        assertThat(history.getBeforeQuantity()).isEqualTo(10);
        assertThat(history.getAfterQuantity()).isEqualTo(15);
    }

    @Test
    void 감소하는_재고_조정_이력을_생성한다() {
        InventoryHistory history = InventoryHistory.adjustment(
                createProductVariant(),
                10,
                3
        );

        assertThat(history.getChangeQuantity()).isEqualTo(-7);
        assertThat(history.getAfterQuantity())
                .isEqualTo(history.getBeforeQuantity() + history.getChangeQuantity());
    }

    @Test
    void 재고_조정_변경량은_0일_수_없다() {
        assertThatThrownBy(
                () -> InventoryHistory.adjustment(createProductVariant(), 10, 10)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("재고 조정 수량은 0일 수 없습니다.");
    }

    @Test
    void 변경_전후_재고는_음수일_수_없다() {
        assertThatThrownBy(
                () -> InventoryHistory.adjustment(createProductVariant(), -1, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("변경 전 재고 수량은 0 이상이어야 합니다.");

        assertThatThrownBy(
                () -> InventoryHistory.adjustment(createProductVariant(), 0, -1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("변경 후 재고 수량은 0 이상이어야 합니다.");
    }

    @Test
    void SKU가_없으면_재고_이력을_생성할_수_없다() {
        assertThatThrownBy(() -> InventoryHistory.initial(null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 SKU는 null일 수 없습니다.");
    }

    private ProductVariant createProductVariant() {
        return new ProductVariant(
                "TEST-001",
                "기본 옵션",
                new BigDecimal("10000"),
                ProductVariantStatus.ON_SALE
        );
    }
}
