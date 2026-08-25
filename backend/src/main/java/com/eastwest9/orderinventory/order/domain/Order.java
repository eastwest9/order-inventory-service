package com.eastwest9.orderinventory.order.domain;

import com.eastwest9.orderinventory.common.persistence.BaseTimeEntity;
import com.eastwest9.orderinventory.common.util.UuidGenerator;
import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderAlreadyCanceledException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    private static final String ORDER_NUMBER_PREFIX = "ORD-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private final List<OrderItem> items = new ArrayList<>();

    public Order(Member member, List<OrderItem> items) {
        validateMember(member);
        validateItems(items);

        this.orderNumber = generateOrderNumber();
        this.member = member;
        this.status = OrderStatus.CREATED;
        this.totalPrice = BigDecimal.ZERO;

        items.forEach(this::addItem);
    }

    public void cancel(LocalDateTime canceledAt) {
        if (status == OrderStatus.CANCELED) {
            throw new OrderAlreadyCanceledException();
        }

        if (canceledAt == null) {
            throw new InvalidOrderException("주문 취소 시각은 필수입니다.");
        }

        status = OrderStatus.CANCELED;
        this.canceledAt = canceledAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    private void addItem(OrderItem item) {
        if (item == null) {
            throw new InvalidOrderException("주문 항목은 null일 수 없습니다.");
        }

        item.assignOrder(this);
        items.add(item);
        totalPrice = totalPrice.add(item.getTotalPrice());
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new InvalidOrderException("주문 회원은 필수입니다.");
        }
    }

    private void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("주문 항목은 하나 이상이어야 합니다.");
        }
    }

    private String generateOrderNumber() {
        return ORDER_NUMBER_PREFIX
                + UuidGenerator.generate().replace("-", "");
    }
}
