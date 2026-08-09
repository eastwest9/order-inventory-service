# Database Convention

## 1. 목적

Order Inventory Service의 데이터베이스 설계 및 명명 기준을 정의한다.

Phase 1에서는 MySQL을 사용하며,
상품·SKU·재고·주문 도메인 설계에 본 규칙을 적용한다.

---

## 2. 기본 원칙

- 테이블과 컬럼은 영문 소문자 `snake_case`를 사용한다.
- 의미가 불명확한 축약어는 사용하지 않는다.
- 동일한 의미의 데이터는 동일한 명칭을 사용한다.
- 필수 데이터는 `NOT NULL`을 사용한다.
- PK와 FK를 명확하게 정의한다.
- DB Schema 변경은 Flyway로 관리한다.

---

## 3. 테이블 명명 규칙

테이블명은 단수형 `snake_case`를 기본으로 한다.

예:

```text
member
product
product_variant
inventory
inventory_history
shop_order
order_item
```

SQL 예약어와 충돌할 수 있는 이름은 사용하지 않는다.

따라서 주문 테이블은 `order` 대신 `shop_order`를 사용한다.

---

## 4. 컬럼 명명 규칙

컬럼명은 `snake_case`를 사용한다.

예:

```text
product_id
product_name
product_status

variant_id
sku_code
sale_price

order_id
order_number
order_status
```

의미가 명확하지 않은 과도한 축약어는 사용하지 않는다.

지양:

```text
prd_nm
ord_st
amt
```

권장:

```text
product_name
order_status
total_amount
```

단, `id`, `sku`처럼 의미가 일반적으로 명확한 용어는 사용할 수 있다.

---

## 5. PK / FK 규칙

### Primary Key

기본 형식:

```text
<entity>_id
```

예:

```text
member_id
product_id
variant_id
inventory_id
order_id
order_item_id
```

기본 타입은 `BIGINT`를 사용하고 자동 증가 방식을 사용한다.

PK는 내부 식별자로 사용하며 업무적 의미를 부여하지 않는다.

예:

```text
order_id
→ DB 내부 식별자

order_number
→ 업무에서 사용하는 주문 번호
```

### Foreign Key

FK 컬럼은 참조 대상 PK의 이름을 그대로 사용한다.

예:

```text
product_variant.product_id
inventory.variant_id
order_item.order_id
order_item.variant_id
```

---

## 6. 데이터 타입 규칙

### 식별자

```text
BIGINT
```

### 문자열

```text
VARCHAR
```

업무 목적에 맞는 최대 길이를 명시한다.

### 수량

```text
INT
```

주문 수량은 1 이상이어야 하며,
재고 수량은 0 이상이어야 한다.

### 금액

```text
DECIMAL(19, 2)
```

금액에는 `FLOAT`, `DOUBLE`을 사용하지 않는다.

Java에서는 `BigDecimal`을 사용한다.

### 날짜 / 시간

```text
DATETIME(6)
```

컬럼명은 `*_at` 형식을 사용한다.

예:

```text
created_at
updated_at
ordered_at
canceled_at
```

DB 저장 시각은 UTC를 기준으로 하고,
화면 표시 시 필요한 timezone으로 변환한다.

---

## 7. 공통 컬럼

변경 가능한 주요 업무 테이블에는 다음 컬럼을 기본으로 사용한다.

```text
created_at
updated_at
```

단순 이력 데이터처럼 생성 후 변경하지 않는 데이터는
`updated_at`을 생략할 수 있다.

---

## 8. NULL 규칙

- 필수값은 `NOT NULL`로 관리한다.
- `NULL`, 빈 문자열, `0`을 같은 의미로 혼용하지 않는다.
- 값이 존재하지 않는 상태를 표현할 때만 `NULL`을 사용한다.

예:

```text
canceled_at = NULL
→ 아직 취소되지 않음
```

---

## 9. 상태값 규칙

상태 컬럼은 다음 형식을 사용한다.

```text
*_status
```

예:

```text
product_status
variant_status
order_status
```

상태값은 의미를 알 수 있는 문자열로 저장한다.

권장:

```text
CREATED
CANCELED
```

지양:

```text
01
02
```

상세 코드값은 `03-code-definitions.md`에서 관리한다.

---

## 10. 삭제 정책

Phase 1에서는 모든 테이블에 공통적으로 `del_yn`과 같은
논리 삭제 컬럼을 추가하지 않는다.

### 상품

판매 여부는 상태값으로 관리한다.

```text
ON_SALE
STOPPED
```

### 주문

주문 데이터는 삭제하지 않고 주문 상태로 관리한다.

```text
CREATED
CANCELED
```

### 재고 이력

재고 이력은 삭제하지 않는다.

---

## 11. Flyway 규칙

DB Schema 변경은 Flyway Migration으로 관리한다.

파일 형식:

```text
V<version>__<description>.sql
```

예:

```text
V1__create_product.sql
V2__create_inventory.sql
V3__create_order.sql
```

이미 `main` 또는 Release에 반영된 Migration 파일은 수정하지 않는다.

변경이 필요한 경우 새로운 Migration을 추가한다.

예:

```text
V4__add_inventory_index.sql
```

---

## 12. Phase 1 대상 테이블

현재 설계 대상은 다음과 같다.

```text
member
product
product_variant
inventory
inventory_history
shop_order
order_item
```

상세 컬럼, 관계, Constraint 및 Index는 ERD 설계 단계에서 확정한다.