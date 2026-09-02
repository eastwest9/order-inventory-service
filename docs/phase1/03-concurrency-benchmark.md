# Phase 1 재고 동시성 Benchmark

## 1. 목적

동일한 주문·재고 업무 규칙과 부하 조건에서 재고 동시성 제어 전략별 정합성과 성능을 비교한다.

첫 번째 실험은 별도의 동시성 제어를 적용하지 않은 `NO_LOCK` baseline이다. 이 문서는 이후 `synchronized`, Optimistic Lock, Pessimistic Lock, Conditional Atomic Update 결과를 같은 조건과 지표로 누적할 수 있도록 구성한다.

## 2. 공통 실험 조건

| 항목 | 값 |
|---|---:|
| Strategy | `NO_LOCK` |
| Initial Inventory | 100 |
| Total Requests | 1,000 |
| Quantity Per Request | 1 |
| Parallelism | 50 |
| Runs | 5 |
| Member ID | 1 |
| Product Variant ID | 3 |
| SKU | `CONCURRENCY-001` |
| Client | PowerShell 7 |
| Parallel execution | `ForEach-Object -Parallel` |
| ThrottleLimit | 50 |

각 Run은 Inventory API로 재고를 100으로 맞춘 후 `maxOrderId`, `maxOrderItemId`, `maxHistoryId`를 기록하고 시작했다. DB 집계는 baseline ID 이후의 실제 row `COUNT`와 `SUM`을 사용했으며 AUTO_INCREMENT ID 차이를 건수로 사용하지 않았다.

PowerShell에서 측정한 TPS는 baseline 정확성 재현을 위한 참고값이다. 전략 간 최종 성능 비교는 모든 전략에 동일한 benchmark harness를 적용해 다시 수행한다.

## 3. 판정 기준

정상 상태에서는 다음 값이 모두 100이어야 한다.

```text
SuccessQuantitySum
= InitialQuantity - FinalQuantity
= ABS(SUM(ORDER history.change_quantity))
= 100
```

- `OVERSOLD`: `SuccessQuantitySum > InitialQuantity`
- `OversoldQuantity`: `SuccessQuantitySum - InitialQuantity`
- `LOST_UPDATE`: `SuccessQuantitySum > ActualInventoryDecrease`
- `LostUpdateGap`: `SuccessQuantitySum - ActualInventoryDecrease`
- `ORDER_HISTORY_MISMATCH`: `SuccessQuantitySum != HistoryDecrease`
- `HTTP_DB_MISMATCH`: `HTTP201 != CommittedOrderCount`

## 4. NO_LOCK baseline

### 4.1 결과

| Run | HTTP201 | Expected409 | UnexpectedHTTP | TransportErrors | CommittedOrders | SuccessQty | HistoryDecrease | FinalQty | ActualDecrease | OversoldQty | LostUpdateGap | ElapsedSec | AttemptedTPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 318 | 682 | 0 | 0 | 318 | 318 | 318 | 0 | 100 | 218 | 218 | 21.510 | 46.49 |
| RUN-02 | 321 | 679 | 0 | 0 | 321 | 321 | 321 | 0 | 100 | 221 | 221 | 13.156 | 76.01 |
| RUN-03 | 321 | 679 | 0 | 0 | 321 | 321 | 321 | 0 | 100 | 221 | 221 | 15.765 | 63.43 |
| RUN-04 | 291 | 709 | 0 | 0 | 291 | 291 | 291 | 0 | 100 | 191 | 191 | 21.079 | 47.44 |
| RUN-05 | 306 | 694 | 0 | 0 | 306 | 306 | 306 | 0 | 100 | 206 | 206 | 12.036 | 83.09 |

### 4.2 집계

| 지표 | 최소 | 최대 | 평균 |
|---|---:|---:|---:|
| HTTP201 | 291 | 321 | 311.4 |
| OversoldQuantity | 191 | 221 | 211.4 |
| LostUpdateGap | 191 | 221 | 211.4 |
| ElapsedSec | 12.036 | 21.510 | 16.709 |
| AttemptedTPS | 46.49 | 83.09 | 63.29 |

- Overselling 발생: 5/5회
- Lost Update 발생: 5/5회

### 4.3 정합성 결과

모든 Run에서 다음 관계가 성립했다.

- `HTTP201 == CommittedOrderCount`
- `CommittedOrderCount == OrderItemCount`
- `OrderItemCount == HistoryCount`
- `SuccessQuantitySum == HistoryDecrease`

반면 모든 Run에서 `SuccessQuantitySum != ActualInventoryDecrease`였다. `UnexpectedHTTP`와 `TransportErrors`는 5회 모두 0이므로 관찰된 불일치는 인프라 또는 클라이언트 오류로 설명되지 않는다.

대표적으로 RUN-02에서는 주문 321건과 ORDER history 321건이 커밋됐고 history 감소 합계도 321이지만, 실제 Inventory는 100에서 0으로 100만 감소했다. 따라서 221개가 초과 판매됐으며 동일한 221만큼 Lost Update가 발생했다.

Inventory가 최종적으로 0이더라도 정상 결과는 아니다. 성공 주문 수량 321이 초기 재고 100을 초과했고, 주문 및 history의 감소량과 실제 Inventory 감소량이 일치하지 않기 때문이다.

### 4.4 중복 history 관찰

동일한 `before_quantity -> after_quantity`가 여러 ORDER history에 반복됐다. 대표적으로 RUN-01에서는 `100 -> 99`가 9건 기록됐다. 이는 여러 트랜잭션이 같은 재고 값을 읽고 각각 주문과 history를 커밋했음을 보여준다.

### 4.5 원인

현재 `NO_LOCK` 흐름은 다음과 같다.

```text
SELECT Inventory
-> Java에서 현재 quantity 확인
-> decrease()
-> Dirty Checking UPDATE
```

동일 Inventory에 여러 트랜잭션이 동시에 접근하면 같은 quantity를 읽을 수 있다.

```text
TX A: quantity 100 조회
TX B: quantity 100 조회
TX A: 99 계산 후 UPDATE
TX B: 99 계산 후 UPDATE
```

두 주문과 두 ORDER history는 각각 커밋되지만, 나중 UPDATE가 앞선 UPDATE와 같은 값으로 덮어써 Inventory에는 1회의 감소만 남는다. 실험에서 `SuccessQuantitySum == HistoryDecrease > ActualInventoryDecrease`가 반복된 이유다.

### 4.6 최종 판정

| 평가 항목 | 판정 |
|---|---|
| Correctness | **FAIL** |
| Overselling | **FAIL** |
| Lost Update | **FAIL** |
| Order ↔ History consistency | **PASS** |
| Order ↔ Inventory consistency | **FAIL** |

`NO_LOCK`은 5회 모두 초과 판매와 Lost Update가 발생했으므로 재고 정합성을 보장하지 못한다.

## 5. 전략별 비교

후속 전략은 공통 실험 조건과 동일한 benchmark harness로 측정한 뒤 아래 표에 추가한다. 결과가 확보되기 전에는 값을 추정하거나 전략을 최종 선택하지 않는다.

| Strategy | Correctness | Overselling Runs | Lost Update Runs | HTTP201 Avg | ElapsedSec Avg | AttemptedTPS Avg |
|---|---|---:|---:|---:|---:|---:|
| NO_LOCK | FAIL | 5/5 | 5/5 | 311.4 | 16.709 | 63.29 |
| SYNCHRONIZED | PASS (3/3) | 0/3 | 0/3 | 100.0 | 21.903 | 46.08 |

## 6. SYNCHRONIZED

### 6.1 구현 구조

주문 생성 요청 앞에 단일 Spring singleton facade의 intrinsic monitor를 두고, 기존 트랜잭션 서비스와 비즈니스 로직은 변경하지 않았다.

```text
OrderController
-> SynchronizedOrderService
-> OrderService @Transactional
```

`SynchronizedOrderService.createOrder()`는 non-transactional `public synchronized` 메서드이며 `OrderService.createOrder()` 호출만 위임한다. 이에 따라 lock과 transaction의 순서는 다음과 같다.

```text
monitor 획득
-> transaction begin
-> 주문/재고/history 처리
-> commit/rollback
-> proxy 반환
-> monitor 해제
```

트랜잭션 proxy가 commit 또는 rollback을 완료한 뒤 facade로 반환하므로, application-level monitor는 트랜잭션 완료까지 유지된다.

### 6.2 실험 조건

| 항목 | 값 |
|---|---:|
| Strategy | `SYNCHRONIZED` |
| Initial Inventory | 100 |
| Total Requests | 1,000 |
| Quantity Per Request | 1 |
| Parallelism | 50 |
| Runs | 3 |
| Member ID | 1 |
| Product Variant ID | 3 |
| SKU | `CONCURRENCY-001` |
| Client | PowerShell 7 |

시간 제약으로 `SYNCHRONIZED` 전략은 동일 조건에서 3회 반복 검증했으며 RUN-04와 RUN-05는 수행하지 않았다.

### 6.3 실행 결과

| Run | HTTP201 | Expected409 | UnexpectedHTTP | TransportErrors | CommittedOrders | OrderItems | SuccessQty | HistoryCount | HistoryDecrease | FinalQty | ActualDecrease | OversoldQty | LostUpdateGap | ElapsedSec | AttemptedTPS | SuccessTPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 900 | 0 | 0 | 100 | 100 | 100 | 100 | 100 | 0 | 100 | 0 | 0 | 24.979 | 40.03 | 4.00 |
| RUN-02 | 100 | 900 | 0 | 0 | 100 | 100 | 100 | 100 | 100 | 0 | 100 | 0 | 0 | 20.308 | 49.24 | 4.92 |
| RUN-03 | 100 | 900 | 0 | 0 | 100 | 100 | 100 | 100 | 100 | 0 | 100 | 0 | 0 | 20.421 | 48.97 | 4.90 |

모든 Run에서 ORDER history의 `SignedHistoryChange`는 -100이었다. 동일한 `before_quantity -> after_quantity` transition의 최대 count는 1이었으며 중복 transition은 없었다.

### 6.4 집계

| 지표 | 최소 | 최대 | 평균 |
|---|---:|---:|---:|
| HTTP201 | 100 | 100 | 100.0 |
| OversoldQuantity | 0 | 0 | 0.0 |
| LostUpdateGap | 0 | 0 | 0.0 |
| ElapsedSec | 20.308 | 24.979 | 21.903 |
| AttemptedTPS | 40.03 | 49.24 | 46.08 |
| SuccessTPS | 4.00 | 4.92 | 4.61 |

### 6.5 정합성 판정

3회 모두 다음 값이 100으로 일치했다.

```text
HTTP201
= CommittedOrderCount
= OrderItemCount
= SuccessQuantitySum
= HistoryCount
= HistoryDecrease
= ActualInventoryDecrease
= 100
```

| 평가 항목 | 판정 |
|---|---|
| Correctness | **PASS (3/3)** |
| Overselling | **0/3** |
| Lost Update | **0/3** |
| History mismatch | **0/3** |
| Order -> History consistency | **PASS** |
| Order -> Inventory consistency | **PASS** |

`UnexpectedHTTP`와 `TransportErrors`도 3회 모두 0이므로 인프라 또는 클라이언트 오류가 정합성 결과에 영향을 준 정황은 없다.

### 6.6 NO_LOCK과 정합성 비교

| Strategy | Correctness | Overselling | Lost Update | Order -> History | Order -> Inventory |
|---|---|---:|---:|---|---|
| NO_LOCK | FAIL | 5/5 | 5/5 | PASS | FAIL |
| SYNCHRONIZED | PASS (3/3) | 0/3 | 0/3 | PASS | PASS |

`NO_LOCK`은 성공 주문과 ORDER history 사이의 수량은 일치했지만, 성공 주문 수량과 실제 Inventory 감소량이 5회 모두 일치하지 않았다. 반면 `SYNCHRONIZED`는 3회 모두 주문, history, 실제 Inventory 감소량이 일치했고 초과 판매와 Lost Update가 관찰되지 않았다.

### 6.7 한계

- 동일 JVM 안에서만 유효하다.
- 동일한 singleton `SynchronizedOrderService` facade를 통과하는 주문 생성만 보호한다.
- scale-out 환경에서는 application instance마다 monitor가 별도로 존재한다.
- 다른 JVM, 다른 프로세스, 직접 DB 변경은 보호하지 못한다.
- 모든 주문 생성을 하나의 monitor로 직렬화하므로 SKU가 달라도 병렬로 처리할 수 없다.

따라서 이번 결과는 단일 JVM 직렬화 비교군의 효과를 보여주지만, 이를 분산 환경의 최종 동시성 전략으로 확대 해석하지 않는다.

### 6.8 성능 해석 주의

PowerShell 7에서 측정한 `ElapsedSec`, `AttemptedTPS`, `SuccessTPS`는 정합성 재현 과정의 참고값이다. `NO_LOCK`과 `SYNCHRONIZED`의 TPS만으로 최종 성능 우열을 확정하지 않는다. 향후 모든 동시성 전략에 동일한 benchmark harness를 적용한 뒤 전략 간 성능을 다시 비교한다.
