# Phase 1 ERD

## 1. 목적

Order Inventory Service Phase 1에서 사용하는 핵심 테이블과 관계를 정의한다.

상세 컬럼 정의는 `05-phase1-table-definition.md`에서 관리한다.

---

## 2. 대상 테이블

- member
- product
- product_variant
- inventory
- inventory_history
- shop_order
- order_item

---

## 3. 전체 관계

Phase 1에서는 다음 관계를 기준으로 설계한다.

```text
member
  1
  │
  N
shop_order
  1
  │
  N
order_item
  N
  │
  1
product_variant
  N
  │
  1
product


product_variant
  1
  │
  1
inventory


product_variant
  1
  │
  N
inventory_history


order_item
  1
  │
  N
inventory_history
```

`inventory_history.order_item_id`는 주문과 관련된 재고 변경일 때만 사용하며,
초기재고, 입고, 재고조정과 같은 변경에서는 `NULL`을 허용한다.

---

## 4. Product : Product Variant

```text
Product
  1
  │
  N
Product Variant
```

하나의 상품은 하나 이상의 Product Variant를 가질 수 있다.

Product는 상품 자체의 정보를 관리한다.

Product Variant는 실제 판매 단위인 SKU를 표현한다.

예:

```text
Product
└─ 무지 티셔츠

Product Variant
├─ 검정 / M
├─ 검정 / L
├─ 흰색 / M
└─ 흰색 / L
```

옵션이 없는 상품도 하나의 기본 Product Variant를 가진다.

가격과 재고는 Product가 아닌 Product Variant를 기준으로 관리한다.

---

## 5. Product Variant : Inventory

```text
Product Variant
  1
  │
  1
Inventory
```

하나의 SKU는 하나의 현재 재고 정보를 가진다.

`inventory`는 SKU의 현재 재고 상태를 관리한다.

재고는 Product가 아닌 Product Variant 단위로 관리한다.

---

## 6. Product Variant : Inventory History

```text
Product Variant
  1
  │
  N
Inventory History
```

하나의 SKU에서는 여러 번의 재고 변경이 발생할 수 있다.

`inventory_history`는 재고의 변경 원인과 변경 전후 수량을 기록한다.

```text
inventory
→ 현재 재고 상태

inventory_history
→ 재고 변경 이력
```

---

## 7. Member : Shop Order

```text
Member
  1
  │
  N
Shop Order
```

하나의 회원은 여러 주문을 생성할 수 있다.

Phase 1에서는 실제 회원가입과 인증 기능을 구현하지 않지만,
주문의 소유자를 표현하기 위해 최소한의 Member 데이터를 사용한다.

---

## 8. Shop Order : Order Item

```text
Shop Order
  1
  │
  N
Order Item
```

하나의 주문은 하나 이상의 주문 상품을 가진다.

예:

```text
Order
├─ SKU A × 2
├─ SKU B × 1
└─ SKU C × 3
```

주문 전체 정보와 개별 주문 상품 정보를 분리하여 관리한다.

---

## 9. Product Variant : Order Item

```text
Product Variant
  1
  │
  N
Order Item
```

하나의 SKU는 여러 주문에 포함될 수 있다.

`order_item`은 주문된 SKU를 참조한다.

상품명과 가격 등 과거 주문 내역 보존이 필요한 정보는
주문 시점의 값을 Snapshot으로 함께 저장한다.

---

## 10. Order Item : Inventory History

주문으로 발생한 재고 변경은 해당 `order_item`과 연결한다.

예:

```text
Order #100

Order Item #1001
SKU A × 2

Order Item #1002
SKU B × 3
```

주문 성공 시:

```text
Inventory History
├─ order_item_id = 1001 / ORDER / -2
└─ order_item_id = 1002 / ORDER / -3
```

주문 취소 시:

```text
Inventory History
├─ order_item_id = 1001 / ORDER_CANCEL / +2
└─ order_item_id = 1002 / ORDER_CANCEL / +3
```

초기재고, 입고, 재고조정처럼 주문과 관계없는 변경에서는
`order_item_id`를 `NULL`로 관리한다.

---

## 11. Phase 1 관계 요약

| Parent | Child | 관계 | 설명 |
|---|---|---|---|
| `product` | `product_variant` | 1:N | 하나의 상품은 여러 SKU를 가질 수 있음 |
| `product_variant` | `inventory` | 1:1 | 하나의 SKU는 하나의 현재 재고를 가짐 |
| `product_variant` | `inventory_history` | 1:N | 하나의 SKU에서 여러 재고 변경 발생 |
| `member` | `shop_order` | 1:N | 한 회원은 여러 주문 가능 |
| `shop_order` | `order_item` | 1:N | 한 주문은 여러 주문 상품 포함 |
| `product_variant` | `order_item` | 1:N | 하나의 SKU가 여러 주문에 포함 가능 |
| `order_item` | `inventory_history` | 1:N | 주문 상품에서 여러 재고 변경이 발생할 수 있음 |