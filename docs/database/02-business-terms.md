# Business Terms

## 1. 목적

Order Inventory Service Phase 1에서 사용하는 핵심 업무 용어의 의미를 정의한다.

코드, DB, API 및 문서에서 동일한 용어를 동일한 의미로 사용하는 것을 목적으로 한다.

---

## 2. 핵심 업무 용어

| 한글 용어 | 영문 용어 | 정의 |
|---|---|---|
| 상품 | Product | 고객에게 판매되는 상위 상품 개념 |
| 상품 판매 단위 | Product Variant | 상품의 옵션 조합 등 실제 판매 가능한 단위 |
| SKU | Stock Keeping Unit | 가격과 재고를 관리하는 실제 판매 단위 |
| SKU 코드 | SKU Code | 각 SKU를 구분하기 위한 고유 업무 코드 |
| 판매 가격 | Sale Price | SKU의 판매 기준 가격 |
| 재고 | Inventory | 특정 SKU가 현재 보유한 수량 |
| 재고 입고 | Inventory Receipt | 외부 공급 등으로 재고가 증가하는 행위 |
| 재고 차감 | Inventory Deduction | 주문으로 인해 재고가 감소하는 행위 |
| 재고 복구 | Inventory Restoration | 주문 취소로 기존 차감 재고를 다시 증가시키는 행위 |
| 재고 조정 | Inventory Adjustment | 관리 목적으로 시스템 재고를 변경하는 행위 |
| 재고 이력 | Inventory History | 재고 변경 원인과 변경 전후 수량을 기록한 데이터 |
| 주문 | Order | 사용자가 하나 이상의 SKU를 구매하기 위해 생성한 거래 |
| 주문 번호 | Order Number | 주문을 업무적으로 식별하기 위한 고유 번호 |
| 주문 상품 | Order Item | 하나의 주문에 포함된 개별 SKU와 주문 수량 정보 |
| 주문 취소 | Order Cancellation | 기존 주문을 취소하고 필요한 재고를 복구하는 행위 |
| 재고 부족 | Insufficient Inventory | 주문 요청 수량보다 주문 가능한 재고가 적은 상태 |
| 동시 주문 | Concurrent Order | 여러 주문 요청이 동시에 재고에 접근하는 상황 |
| 재고 정합성 | Inventory Consistency | 주문, 현재 재고 및 재고 이력 간 수량 관계가 일치하는 상태 |
| 초과 판매 | Overselling | 실제 주문 가능한 재고보다 많은 주문이 성공한 상태 |

---

## 3. Product와 SKU의 구분

Product와 SKU는 동일한 개념으로 사용하지 않는다.

예:

```text
Product
└─ 기본 티셔츠

SKU
├─ 검정 / M
├─ 검정 / L
├─ 흰색 / M
└─ 흰색 / L
```

가격과 재고는 Product가 아니라 SKU를 기준으로 관리한다.

옵션이 없는 상품도 하나의 기본 SKU를 가진다.

---

## 4. Inventory와 Inventory History의 구분

```text
Inventory
→ 현재 재고 상태

Inventory History
→ 현재 재고가 변경된 과정
```

예:

```text
초기재고   +100
주문        -10
입고        +20
주문        -30
----------------
현재재고      80
```

`inventory`에는 현재 수량을 저장하고,
`inventory_history`에는 각각의 변경 내역을 저장한다.

---

## 5. Order와 Order Item의 구분

하나의 주문은 여러 개의 주문 상품을 포함할 수 있다.

```text
Order
├─ Order Item A × 2
├─ Order Item B × 1
└─ Order Item C × 3
```

따라서:

```text
shop_order
→ 주문 전체 정보

order_item
→ 주문에 포함된 각각의 SKU 정보
```

로 구분한다.

---

## 6. Phase 1 범위 외 용어

다음 개념은 Phase 1에서 정의하지 않는다.

```text
장바구니
결제
환불
재고 예약
출고
송장
배송
반품
쿠폰
리뷰
```

필요한 Phase에서 별도로 정의한다.