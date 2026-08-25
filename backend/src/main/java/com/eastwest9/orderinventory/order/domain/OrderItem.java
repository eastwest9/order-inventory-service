package com.eastwest9.orderinventory.order.domain;

import com.eastwest9.orderinventory.common.persistence.BaseCreatedEntity;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", nullable = false, length = 200)
    private String variantName;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    public OrderItem(ProductVariant productVariant, int quantity) {
        validateProductVariant(productVariant);
        validateQuantity(quantity);

        this.productVariant = productVariant;
        this.productName = productVariant.getProduct().getName();
        this.variantName = productVariant.getName();
        this.unitPrice = productVariant.getSalePrice();
        this.quantity = quantity;
        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void assignOrder(Order order) {
        if (order == null) {
            throw new InvalidOrderException("주문은 필수입니다.");
        }

        if (this.order != null && this.order != order) {
            throw new InvalidOrderException("주문 항목은 다른 주문에 다시 할당할 수 없습니다.");
        }

        this.order = order;
    }

    private void validateProductVariant(ProductVariant productVariant) {
        if (productVariant == null) {
            throw new InvalidOrderException("상품 SKU는 필수입니다.");
        }

        if (productVariant.getProduct() == null) {
            throw new InvalidOrderException("상품에 연결된 SKU만 주문할 수 있습니다.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidOrderException("주문 수량은 1 이상이어야 합니다.");
        }
    }
}
