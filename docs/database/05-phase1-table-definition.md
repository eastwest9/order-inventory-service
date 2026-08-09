# Phase 1 Table Definition

## 1. 목적

Order Inventory Service Phase 1에서 사용하는 테이블의
컬럼, 데이터 타입, Key 및 주요 제약조건을 정의한다.

Phase 1 대상 테이블은 다음과 같다.

```text
member
product
product_variant
inventory
inventory_history
shop_order
order_item
```

---

# 2. product

상품의 기본 정보를 관리한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `product_id` | BIGINT | N | PK | 상품 내부 식별자 |
| `product_name` | VARCHAR(200) | N | | 상품명 |
| `product_status` | VARCHAR(20) | N | | 상품 판매 상태 |
| `created_at` | DATETIME(6) | N | | 생성일시 |
| `updated_at` | DATETIME(6) | N | | 수정일시 |

## 제약조건

- `product_id`는 자동 증가 PK를 사용한다.
- `product_name`은 필수값이다.
- `product_status`는 `ON_SALE`, `STOPPED` 중 하나를 사용한다.

## 설계 결정

상품의 가격과 재고는 `product`에서 관리하지 않는다.

```text
product
→ 상품 자체 정보

product_variant
→ 실제 판매 단위 및 판매 가격

inventory
→ SKU별 현재 재고
```

---

# 3. product_variant

상품의 실제 판매 단위인 SKU 정보를 관리한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `variant_id` | BIGINT | N | PK | SKU 내부 식별자 |
| `product_id` | BIGINT | N | FK | 소속 상품 식별자 |
| `sku_code` | VARCHAR(100) | N | UK | SKU 고유 코드 |
| `variant_name` | VARCHAR(200) | N | | SKU 표시명 |
| `sale_price` | DECIMAL(19, 2) | N | | 판매 가격 |
| `variant_status` | VARCHAR(20) | N | | SKU 판매 상태 |
| `created_at` | DATETIME(6) | N | | 생성일시 |
| `updated_at` | DATETIME(6) | N | | 수정일시 |

## 제약조건

- `variant_id`는 자동 증가 PK를 사용한다.
- `product_id`는 `product.product_id`를 참조한다.
- `sku_code`는 중복될 수 없다.
- `sale_price`는 0 이상이어야 한다.
- `variant_status`는 `ON_SALE`, `STOPPED` 중 하나를 사용한다.

## 설계 결정

`sku_code`는 DB 내부 식별자인 `variant_id`와 별개의 업무 식별자이다.

예:

```text
TSHIRT-BLACK-M
TSHIRT-BLACK-L
TSHIRT-WHITE-M
```

`variant_name`은 SKU를 사람이 구분하기 위한 표시명이다.

예:

```text
검정 / M
검정 / L
흰색 / M
```

Phase 1에서는 옵션 시스템 자체가 핵심이 아니므로
별도의 옵션 및 옵션값 테이블을 만들지 않는다.

옵션이 없는 상품도 하나의 기본 SKU를 가진다.

판매 가격은 SKU 단위로 관리한다.

---

# 4. inventory

SKU별 현재 재고를 관리한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `inventory_id` | BIGINT | N | PK | 재고 내부 식별자 |
| `variant_id` | BIGINT | N | FK, UK | SKU 식별자 |
| `quantity` | INT | N | | 현재 재고 수량 |
| `version` | BIGINT | N | | Optimistic Lock용 버전 |
| `created_at` | DATETIME(6) | N | | 생성일시 |
| `updated_at` | DATETIME(6) | N | | 수정일시 |

## 제약조건

- `inventory_id`는 자동 증가 PK를 사용한다.
- `variant_id`는 `product_variant.variant_id`를 참조한다.
- `variant_id`는 중복될 수 없다.
- `quantity`는 0 이상이어야 한다.
- `version`은 0 이상이어야 한다.

## 설계 결정

Product Variant와 Inventory는 1:1 관계이다.

```text
product_variant 1 : 1 inventory
```

하나의 SKU에 현재 재고 레코드가 여러 개 존재하지 않도록
`variant_id`에 Unique Constraint를 적용한다.

`quantity`는 현재 시점의 재고 수량이다.

`version`은 Phase 1 Optimistic Lock 실험에서 사용한다.

특정 동시성 전략을 미리 최종 방식으로 결정하지 않고,
동일한 테스트 조건에서 여러 전략을 비교한다.

---

# 5. inventory_history

SKU별 재고 변경 이력을 관리한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `history_id` | BIGINT | N | PK | 재고 이력 식별자 |
| `variant_id` | BIGINT | N | FK | SKU 식별자 |
| `order_item_id` | BIGINT | Y | FK | 관련 주문 상품 식별자 |
| `change_type` | VARCHAR(30) | N | | 재고 변경 유형 |
| `change_quantity` | INT | N | | 재고 증감 수량 |
| `before_quantity` | INT | N | | 변경 전 재고 |
| `after_quantity` | INT | N | | 변경 후 재고 |
| `created_at` | DATETIME(6) | N | | 변경 발생일시 |

## 제약조건

- `history_id`는 자동 증가 PK를 사용한다.
- `variant_id`는 `product_variant.variant_id`를 참조한다.
- `order_item_id`는 `order_item.order_item_id`를 참조하며 주문과 관련없는 변경에서는 `NULL`을 허용한다.
- `before_quantity`는 0 이상이어야 한다.
- `after_quantity`는 0 이상이어야 한다.
- 재고 이력은 생성 후 수정하지 않는 것을 원칙으로 한다.

## Change Type

```text
INITIAL
RECEIPT
ORDER
ORDER_CANCEL
ADJUSTMENT
```

`change_quantity`는 증감을 부호로 표현한다.

예:

```text
초기재고
change_quantity = 100

입고
change_quantity = 10

주문
change_quantity = -3

주문 취소
change_quantity = 3

재고조정
change_quantity = -2
```

## 주문과 재고 이력

주문과 관련된 재고 변경은 `order_item_id`를 사용하여
어떤 주문 상품 때문에 재고가 변경됐는지 추적한다.

예:

```text
change_type = ORDER
order_item_id = 1001
change_quantity = -3
```

주문 취소:

```text
change_type = ORDER_CANCEL
order_item_id = 1001
change_quantity = 3
```

초기재고, 입고, 재고조정과 같이 주문과 관계없는 경우:

```text
order_item_id = NULL
```

을 사용한다.

---

# 6. member

주문의 소유자를 관리한다.

Phase 1에서는 인증과 인가 기능을 구현하지 않으며,
주문 관계를 표현하기 위한 최소한의 회원 정보만 사용한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `member_id` | BIGINT | N | PK | 회원 내부 식별자 |
| `member_name` | VARCHAR(100) | N | | 회원명 |
| `created_at` | DATETIME(6) | N | | 생성일시 |
| `updated_at` | DATETIME(6) | N | | 수정일시 |

## 제약조건

- `member_id`는 자동 증가 PK를 사용한다.
- `member_name`은 필수값이다.

## 설계 결정

Phase 1에서는 다음 기능을 구현하지 않는다.

```text
회원가입
로그인
비밀번호 관리
Spring Security
Role 기반 권한 관리
Token 인증
```

회원 및 인증 도메인은 후속 Phase에서 확장한다.

---

# 7. shop_order

주문 전체 정보를 관리한다.

`order`는 SQL 예약어와 충돌할 가능성이 있으므로
테이블명은 `shop_order`를 사용한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `order_id` | BIGINT | N | PK | 주문 내부 식별자 |
| `order_number` | VARCHAR(50) | N | UK | 업무용 주문 번호 |
| `member_id` | BIGINT | N | FK | 주문 회원 식별자 |
| `order_status` | VARCHAR(20) | N | | 주문 상태 |
| `total_amount` | DECIMAL(19, 2) | N | | 주문 총 금액 |
| `canceled_at` | DATETIME(6) | Y | | 주문 취소일시 |
| `created_at` | DATETIME(6) | N | | 주문 생성일시 |
| `updated_at` | DATETIME(6) | N | | 주문 수정일시 |

## 제약조건

- `order_id`는 자동 증가 PK를 사용한다.
- `order_number`는 중복될 수 없다.
- `member_id`는 `member.member_id`를 참조한다.
- `total_amount`는 0 이상이어야 한다.
- `order_status`는 Phase 1에서 `CREATED`, `CANCELED`를 사용한다.

## 설계 결정

`order_id`와 `order_number`의 역할을 구분한다.

```text
order_id
→ DB 내부 식별자

order_number
→ 외부 및 업무에서 사용하는 주문 번호
```

Phase 1에서는 주문 생성 시점과 데이터 생성 시점이 동일하므로
별도의 `ordered_at` 컬럼을 사용하지 않고 `created_at`을 주문 생성 시각으로 사용한다.

주문 취소는 데이터를 삭제하지 않고 상태 변경으로 관리한다.

```text
CREATED
   ↓
CANCELED
```

정상 주문에서는:

```text
canceled_at = NULL
```

취소 완료 시:

```text
order_status = CANCELED
canceled_at = 실제 취소 시각
```

으로 관리한다.

---

# 8. order_item

하나의 주문에 포함된 개별 SKU 정보를 관리한다.

| 컬럼명 | 데이터 타입 | NULL | Key | 설명 |
|---|---|---|---|---|
| `order_item_id` | BIGINT | N | PK | 주문 상품 식별자 |
| `order_id` | BIGINT | N | FK | 주문 식별자 |
| `variant_id` | BIGINT | N | FK | 주문한 SKU 식별자 |
| `product_name` | VARCHAR(200) | N | | 주문 시점 상품명 Snapshot |
| `variant_name` | VARCHAR(200) | N | | 주문 시점 SKU명 Snapshot |
| `unit_price` | DECIMAL(19, 2) | N | | 주문 시점 단가 |
| `quantity` | INT | N | | 주문 수량 |
| `total_price` | DECIMAL(19, 2) | N | | 주문 상품 총 금액 |
| `created_at` | DATETIME(6) | N | | 생성일시 |

## 제약조건

- `order_item_id`는 자동 증가 PK를 사용한다.
- `order_id`는 `shop_order.order_id`를 참조한다.
- `variant_id`는 `product_variant.variant_id`를 참조한다.
- `unit_price`는 0 이상이어야 한다.
- `quantity`는 1 이상이어야 한다.
- `total_price`는 0 이상이어야 한다.

## 설계 결정

주문 상품에는 주문 시점의 상품 정보와 가격을 Snapshot으로 저장한다.

예를 들어 주문 당시:

```text
상품명 = 무지 티셔츠
가격 = 29,000원
```

이었는데 이후:

```text
상품명 = 베이직 티셔츠
가격 = 35,000원
```

으로 변경되더라도 기존 주문 내역은 변경되지 않아야 한다.

따라서 `order_item`에는 다음 정보를 저장한다.

```text
product_name
variant_name
unit_price
quantity
total_price
```

`variant_id`는 현재 SKU와의 연결 및 추적을 위해 유지한다.

Phase 1에서는:

```text
total_price = unit_price × quantity
```

관계를 유지한다.

또한:

```text
shop_order.total_amount
=
SUM(order_item.total_price)
```

관계를 유지한다.

---

# 9. 주요 관계 정리

| Parent | Child | 관계 | 설명 |
|---|---|---|---|
| `product` | `product_variant` | 1:N | 하나의 상품은 여러 SKU를 가질 수 있음 |
| `product_variant` | `inventory` | 1:1 | 하나의 SKU는 하나의 현재 재고를 가짐 |
| `product_variant` | `inventory_history` | 1:N | 하나의 SKU에서 여러 재고 변경 발생 |
| `member` | `shop_order` | 1:N | 하나의 회원은 여러 주문 가능 |
| `shop_order` | `order_item` | 1:N | 하나의 주문은 여러 주문 상품 포함 |
| `product_variant` | `order_item` | 1:N | 하나의 SKU가 여러 주문에 포함 가능 |
| `order_item` | `inventory_history` | 1:N | 하나의 주문 상품에서 주문/취소 재고 이력이 발생 가능 |

---

# 10. 주문 생성 Transaction

주문 생성 시 다음 작업은 하나의 Transaction으로 처리한다.

```text
shop_order 생성
        ↓
order_item 생성
        ↓
inventory 재고 차감
        ↓
inventory_history 생성
```

하나라도 실패하면 전체 작업을 Rollback한다.

즉 다음과 같은 부분 성공 상태를 허용하지 않는다.

```text
주문은 생성됐지만 재고가 차감되지 않음
재고는 차감됐지만 주문이 생성되지 않음
재고는 차감됐지만 재고 이력이 없음
```

---

# 11. 주문 취소 Transaction

주문 취소 시 다음 작업도 하나의 Transaction으로 처리한다.

```text
shop_order 상태 변경
        ↓
inventory 재고 복구
        ↓
inventory_history 생성
```

하나라도 실패하면 전체 작업을 Rollback한다.

이미 `CANCELED` 상태인 주문은 다시 취소할 수 없다.

---

# 12. Phase 1 핵심 정합성 기준

동시 주문 처리 이후 다음 관계가 일치해야 한다.

```text
성공 주문 수량 합계
=
초기 재고 - 최종 재고
=
ORDER 유형 재고 차감 수량 합계
```

대표 테스트 조건:

```text
초기 재고       100
동시 주문 요청  1,000
요청당 수량       1
```

기대 결과:

```text
성공 주문        100
실패 주문        900
최종 재고          0
```

다음 조건을 모두 만족해야 한다.

- 재고는 음수가 될 수 없다.
- 보유 재고보다 많은 주문이 성공해서는 안 된다.
- 주문 실패 시 관련 데이터 변경은 모두 Rollback되어야 한다.
- 주문 성공 시 재고 변경 이력이 기록되어야 한다.
- 주문 취소 시 차감한 재고가 복구되어야 한다.
- 동일 주문이 중복 취소되어 재고가 두 번 복구되어서는 안 된다.
- `shop_order.total_amount`와 주문 상품 금액 합계가 일치해야 한다.

---

# 13. Phase 1 제외 범위

현재 테이블 설계에는 다음 기능을 포함하지 않는다.

```text
장바구니
회원 인증 / 인가
결제
재고 예약
배송
송장
반품
환불
쿠폰
리뷰
```

필요한 기능은 후속 Phase에서 별도로 설계한다.