package com.eastwest9.orderinventory.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderAlreadyCanceledException;
import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderDomainTest {

    @Test
    void 정상_주문_항목을_생성한다() {
        ProductVariant variant = createProductVariant(
                "SKU-001",
                "기본 옵션",
                "10000"
        );

        OrderItem item = new OrderItem(variant, 2);

        assertThat(item.getProductVariant()).isEqualTo(variant);
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void 주문_수량이_0이면_주문_항목을_생성할_수_없다() {
        assertThatThrownBy(() -> new OrderItem(
                createProductVariant("SKU-001", "기본 옵션", "10000"),
                0
        ))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 수량은 1 이상이어야 합니다.");
    }

    @Test
    void 주문_수량이_음수이면_주문_항목을_생성할_수_없다() {
        assertThatThrownBy(() -> new OrderItem(
                createProductVariant("SKU-001", "기본 옵션", "10000"),
                -1
        ))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 수량은 1 이상이어야 합니다.");
    }

    @Test
    void 주문_항목은_주문_시점의_상품_정보를_snapshot으로_보관한다() {
        ProductVariant variant = createProductVariant(
                "SKU-001",
                "라지",
                "12900.50"
        );

        OrderItem item = new OrderItem(variant, 1);

        assertThat(item.getProductName()).isEqualTo("테스트 상품");
        assertThat(item.getVariantName()).isEqualTo("라지");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("12900.50");
    }

    @Test
    void 주문_항목_총액은_단가와_수량을_곱해_계산한다() {
        OrderItem item = new OrderItem(
                createProductVariant("SKU-001", "기본 옵션", "12900.50"),
                3
        );

        assertThat(item.getTotalPrice()).isEqualByComparingTo("38701.50");
    }

    @Test
    void 정상_주문을_생성한다() {
        Member member = new Member("테스트 회원");
        OrderItem item = createOrderItem("SKU-001", "기본 옵션", "10000", 1);

        Order order = new Order(member, List.of(item));

        assertThat(order.getMember()).isEqualTo(member);
        assertThat(order.getItems()).containsExactly(item);
        assertThat(order.getOrderNumber())
                .startsWith("ORD-")
                .hasSize(36);
    }

    @Test
    void 주문의_초기_상태는_CREATED이다() {
        Order order = createOrder();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getCanceledAt()).isNull();
    }

    @Test
    void 주문_항목은_하나_이상이어야_한다() {
        assertThatThrownBy(() -> new Order(new Member("테스트 회원"), List.of()))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 항목은 하나 이상이어야 합니다.");
    }

    @Test
    void 주문_항목이_null이면_주문을_생성할_수_없다() {
        assertThatThrownBy(() -> new Order(new Member("테스트 회원"), null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 항목은 하나 이상이어야 합니다.");
    }

    @Test
    void 여러_주문_항목의_총액을_합산한다() {
        OrderItem first = createOrderItem(
                "SKU-001", "첫 번째 옵션", "10000", 2
        );
        OrderItem second = createOrderItem(
                "SKU-002", "두 번째 옵션", "2500.50", 3
        );

        Order order = new Order(
                new Member("테스트 회원"),
                List.of(first, second)
        );

        assertThat(order.getTotalPrice()).isEqualByComparingTo("27501.50");
    }

    @Test
    void 주문을_취소하면_CREATED에서_CANCELED로_변경된다() {
        Order order = createOrder();

        order.cancel(LocalDateTime.of(2026, 8, 26, 12, 0));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 주문_취소_시각을_전달받아_설정한다() {
        Order order = createOrder();
        LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 26, 12, 0);

        order.cancel(canceledAt);

        assertThat(order.getCanceledAt()).isEqualTo(canceledAt);
    }

    @Test
    void 취소된_주문은_다시_취소할_수_없다() {
        Order order = createOrder();
        LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 26, 12, 0);
        order.cancel(canceledAt);

        assertThatThrownBy(() -> order.cancel(canceledAt.plusMinutes(1)))
                .isInstanceOf(OrderAlreadyCanceledException.class)
                .hasMessage("이미 취소된 주문입니다.");
    }

    @Test
    void 주문과_주문_항목의_양방향_연관관계를_설정한다() {
        OrderItem item = createOrderItem(
                "SKU-001", "기본 옵션", "10000", 1
        );

        Order order = new Order(new Member("테스트 회원"), List.of(item));

        assertThat(item.getOrder()).isSameAs(order);
        assertThat(order.getItems()).containsExactly(item);
    }

    @Test
    void 주문_회원은_필수이다() {
        OrderItem item = createOrderItem(
                "SKU-001", "기본 옵션", "10000", 1
        );

        assertThatThrownBy(() -> new Order(null, List.of(item)))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 회원은 필수입니다.");
    }

    @Test
    void 주문_취소_시각은_필수이다() {
        Order order = createOrder();

        assertThatThrownBy(() -> order.cancel(null))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("주문 취소 시각은 필수입니다.");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void 주문_항목_목록을_외부에서_변경할_수_없다() {
        Order order = createOrder();

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("10000");
    }

    private Order createOrder() {
        return new Order(
                new Member("테스트 회원"),
                List.of(createOrderItem(
                        "SKU-001", "기본 옵션", "10000", 1
                ))
        );
    }

    private OrderItem createOrderItem(String skuCode, String variantName, String salePrice, int quantity) {
        return new OrderItem(
                createProductVariant(skuCode, variantName, salePrice),
                quantity
        );
    }

    private ProductVariant createProductVariant(String skuCode, String variantName, String salePrice) {
        Product product = new Product(
                "테스트 상품",
                ProductStatus.ON_SALE
        );
        ProductVariant variant = new ProductVariant(
                skuCode,
                variantName,
                new BigDecimal(salePrice),
                ProductVariantStatus.ON_SALE
        );
        product.addVariant(variant);

        return variant;
    }
}
