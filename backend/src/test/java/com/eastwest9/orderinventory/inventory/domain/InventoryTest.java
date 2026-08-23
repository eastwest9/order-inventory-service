package com.eastwest9.orderinventory.inventory.domain;

import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryTest {

    @Test
    void 초기_재고를_생성한다() {
        ProductVariant productVariant = createProductVariant();

        Inventory inventory = new Inventory(productVariant, 0);

        assertThat(inventory.getProductVariant()).isEqualTo(productVariant);
        assertThat(inventory.getQuantity()).isZero();
    }

    @Test
    void 초기_재고는_음수일_수_없다() {
        assertThatThrownBy(() -> new Inventory(createProductVariant(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("초기 재고 수량은 0 이상이어야 합니다.");
    }

    @Test
    void SKU가_없으면_초기_재고를_생성할_수_없다() {
        assertThatThrownBy(() -> new Inventory(null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품 SKU는 null일 수 없습니다.");
    }

    @Test
    void 재고를_입고한다() {
        Inventory inventory = new Inventory(createProductVariant(), 10);

        inventory.receive(5);

        assertThat(inventory.getQuantity()).isEqualTo(15);
    }

    @Test
    void 입고_수량은_1_이상이어야_한다() {
        Inventory inventory = new Inventory(createProductVariant(), 10);

        assertThatThrownBy(() -> inventory.receive(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입고 수량은 1 이상이어야 합니다.");
    }

    @Test
    void 입고_결과가_int_범위를_초과할_수_없다() {
        Inventory inventory = new Inventory(
                createProductVariant(),
                Integer.MAX_VALUE
        );

        assertThatThrownBy(() -> inventory.receive(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("입고 후 재고 수량이 허용 범위를 초과합니다.");

        assertThat(inventory.getQuantity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void 재고를_조정한다() {
        Inventory inventory = new Inventory(createProductVariant(), 10);

        inventory.adjust(3);

        assertThat(inventory.getQuantity()).isEqualTo(3);
    }

    @Test
    void 조정_재고는_음수일_수_없다() {
        Inventory inventory = new Inventory(createProductVariant(), 10);

        assertThatThrownBy(() -> inventory.adjust(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("조정 재고 수량은 0 이상이어야 합니다.");
    }

    @Test
    void 현재_재고와_동일한_수량으로_조정할_수_없다() {
        Inventory inventory = new Inventory(createProductVariant(), 10);

        assertThatThrownBy(() -> inventory.adjust(10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 재고와 동일한 수량으로 조정할 수 없습니다.");
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
