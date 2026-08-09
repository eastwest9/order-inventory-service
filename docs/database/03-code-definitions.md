# Code Definitions

## 1. 목적

Order Inventory Service Phase 1에서 사용하는 상태 및 코드값을 정의한다.

상태값은 의미를 알 수 있는 문자열 형태로 관리한다.

---

## 2. Product Status

상품의 판매 상태를 나타낸다.

| Code | 한글명 | 설명 |
|---|---|---|
| `ON_SALE` | 판매중 | 신규 주문이 가능한 상품 |
| `STOPPED` | 판매중지 | 신규 주문이 불가능한 상품 |

상태 전이:

```text
ON_SALE
   ↕
STOPPED
```

---

## 3. Product Variant Status

SKU의 판매 상태를 나타낸다.

| Code | 한글명 | 설명 |
|---|---|---|
| `ON_SALE` | 판매중 | 신규 주문이 가능한 SKU |
| `STOPPED` | 판매중지 | 신규 주문이 불가능한 SKU |

주문 가능 조건:

```text
Product = ON_SALE
AND
Product Variant = ON_SALE
```

---

## 4. Order Status

Phase 1의 주문 상태를 나타낸다.

| Code | 한글명 | 설명 |
|---|---|---|
| `CREATED` | 주문생성 | 주문 생성과 재고 차감이 완료된 상태 |
| `CANCELED` | 주문취소 | 주문 취소와 재고 복구가 완료된 상태 |

상태 전이:

```text
CREATED
   ↓
CANCELED
```

이미 `CANCELED` 상태인 주문은 다시 취소할 수 없다.

결제 및 배송 상태는 Phase 1에서 정의하지 않는다.

---

## 5. Inventory Change Type

재고가 변경된 원인을 나타낸다.

| Code | 한글명 | 방향 | 설명 |
|---|---|---|---|
| `INITIAL` | 초기재고 | 증가 | 최초 재고 설정 |
| `RECEIPT` | 입고 | 증가 | 외부 공급 등으로 재고 증가 |
| `ORDER` | 주문차감 | 감소 | 주문 성공으로 재고 감소 |
| `ORDER_CANCEL` | 주문취소복구 | 증가 | 주문 취소로 재고 복구 |
| `ADJUSTMENT` | 재고조정 | 증가/감소 | 관리 목적으로 재고 조정 |

---

## 6. 코드 작성 규칙

코드값은 대문자 `UPPER_SNAKE_CASE`를 사용한다.

예:

```text
ON_SALE
ORDER_CANCEL
```

의미를 알 수 없는 숫자 코드는 사용하지 않는다.

지양:

```text
01
02
03
```

권장:

```text
CREATED
CANCELED
```

한번 사용한 코드의 의미를 임의로 변경하지 않는다.

새로운 의미가 필요한 경우 새로운 코드 추가를 검토한다.

---

## 7. Java Enum 매핑

Java에서는 Enum으로 정의한다.

예:

```java
public enum ProductStatus {
    ON_SALE,
    STOPPED
}
```

```java
public enum OrderStatus {
    CREATED,
    CANCELED
}
```

```java
public enum InventoryChangeType {
    INITIAL,
    RECEIPT,
    ORDER,
    ORDER_CANCEL,
    ADJUSTMENT
}
```

JPA에서는 문자열로 저장한다.

```java
@Enumerated(EnumType.STRING)
```

`EnumType.ORDINAL`은 사용하지 않는다.