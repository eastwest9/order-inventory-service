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

각 전략은 공통 실험 조건과 동일한 benchmark harness로 측정한다. 결과가 확보되기 전에는 값을 추정하거나 전략을 최종 선택하지 않는다.

| Strategy | Correctness | Overselling Runs | Lost Update Runs | HTTP201 Avg | ElapsedSec Avg | AttemptedTPS Avg | SuccessTPS Avg | Conflict | 특징 |
|---|---|---:|---:|---:|---:|---:|---:|---|---|
| NO_LOCK | FAIL | 5/5 | 5/5 | 311.4 | 16.709 | 63.29 | 미기록 | 해당 없음 | 동시 UPDATE의 Lost Update로 초과 판매 발생 |
| SYNCHRONIZED | PASS (3/3) | 0/3 | 0/3 | 100.0 | 21.903 | 46.08 | 4.61 | 없음 | 단일 JVM monitor로 주문 생성 직렬화 |
| OPTIMISTIC_NO_RETRY | PASS (3/3) | 0/3 | 0/3 | 100 | 13.899 | 73.47 | 7.35 | 평균 235건 (23.5%) | `@Version` 충돌 감지, retry 없음 |

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

## 7. OPTIMISTIC_NO_RETRY

### 7.1 구현 구조

Inventory entity의 `version` 필드에 JPA Optimistic Lock을 활성화했다.

```java
@Version
@Column(name = "version", nullable = false)
private Long version;
```

`inventory.version` 컬럼은 기존 `V1__create_initial_schema.sql`부터 `BIGINT NOT NULL DEFAULT 0`으로 존재하므로 신규 migration은 추가하지 않았다.

활성 주문 생성 경로는 다음과 같다.

```text
OrderController
-> OrderService.createOrder()
-> @Transactional
```

`SynchronizedOrderService`는 코드에 남아 있지만 이번 전략의 active path에서는 사용하지 않았다. Retry, Pessimistic Lock, Conditional Atomic Update, `@Lock`은 적용하지 않았으며 주문 생성 경로에서 명시적인 Inventory flush도 호출하지 않았다.

Optimistic 충돌은 다음 HTTP 응답으로 변환했다.

```text
OptimisticLockingFailureException
-> HTTP 409
-> INVENTORY_CONFLICT
```

### 7.2 실험 조건

| 항목 | 값 |
|---|---:|
| Strategy | `OPTIMISTIC_NO_RETRY` |
| Initial Inventory | 100 |
| Total Requests | 1,000 |
| Quantity Per Request | 1 |
| Parallelism | 50 |
| TimeoutSec | 30 |
| Runs | 3 |
| Member ID | 1 |
| Product ID | 3 |
| Product Variant ID | 3 |
| SKU | `CONCURRENCY-001` |
| Client | PowerShell 7 |
| Parallel execution | `ForEach-Object -Parallel` |
| ThrottleLimit | 50 |

`NO_LOCK`, `SYNCHRONIZED`와 동일한 SKU 및 부하 조건을 유지했다. 시간 제약으로 이 전략도 3회 반복 검증했다.

### 7.3 실행 결과

#### 7.3.1 HTTP 결과

| Run | HTTP201 | InsufficientInventory409 | InventoryConflict409 | Unexpected409 | UnexpectedHTTP | TransportErrors | ConflictRatePct | ElapsedSec | AttemptedTPS | SuccessTPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 659 | 241 | 0 | 0 | 0 | 24.1% | 14.690 | 68.08 | 6.81 |
| RUN-02 | 100 | 672 | 228 | 0 | 0 | 0 | 22.8% | 15.754 | 63.48 | 6.35 |
| RUN-03 | 100 | 664 | 236 | 0 | 0 | 0 | 23.6% | 11.254 | 88.86 | 8.89 |

각 Run에서 `HTTP201 + InsufficientInventory409 + InventoryConflict409 + Unexpected409 + UnexpectedHTTP + TransportErrors = 1,000`이었다.

#### 7.3.2 Inventory 및 version 결과

| Run | InitialQty | FinalQty | ActualDecrease | VersionBefore | VersionAfter | VersionIncrease | UtilizationPct |
|---|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 0 | 100 | 1 | 101 | 100 | 100% |
| RUN-02 | 100 | 0 | 100 | 102 | 202 | 100 | 100% |
| RUN-03 | 100 | 0 | 100 | 203 | 303 | 100 | 100% |

모든 Run에서 `VersionIncrease`는 100으로, 실제 Inventory 변경 성공 횟수 및 `ActualInventoryDecrease`와 일치했다.

#### 7.3.3 DB 정합성 결과

| Run | CommittedOrders | OrderItems | SuccessQty | HistoryCount | SignedHistoryChange | HistoryDecrease | MaxSameTransition | DuplicateTransition | Correctness | Oversold | LostUpdate |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|---|---|
| RUN-01 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | false | PASS | false | false |
| RUN-02 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | false | PASS | false | false |
| RUN-03 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | false | PASS | false | false |

세 Run 모두 CANCELED 주문과 `ORDER_CANCEL` history는 0건이었다. Committed `ORDER` history에서 동일한 `before_quantity -> after_quantity -> change_quantity` transition의 최대 count는 1이었고 중복 transition은 없었다.

### 7.4 3회 집계

| 지표 | 최소 | 최대 | 평균 |
|---|---:|---:|---:|
| HTTP201 | 100 | 100 | 100 |
| InsufficientInventory409 | 659 | 672 | 665 |
| InventoryConflict409 | 228 | 241 | 235 |
| ConflictRatePct | 22.8% | 24.1% | 23.5% |
| ElapsedSec | 11.254 | 15.754 | 13.899 |
| AttemptedTPS | 63.48 | 88.86 | 73.47 |
| SuccessTPS | 6.35 | 8.89 | 7.35 |
| VersionIncrease | 100 | 100 | 100 |

- Correctness: **PASS (3/3)**
- Overselling: **0/3**
- Lost Update: **0/3**
- Utilization 100%: **3/3**
- Unexpected409: **총 0건**
- UnexpectedHTTP: **총 0건**
- TransportErrors: **총 0건**

### 7.5 정합성 판정

3회 모두 다음 관계가 성립했다.

```text
HTTP201
= CommittedOrderCount
= OrderItemCount
= SuccessQuantitySum
= HistoryCount
= HistoryDecrease
= ActualInventoryDecrease
= VersionIncrease
= 100
```

| 평가 항목 | 판정 |
|---|---|
| Correctness | **PASS (3/3)** |
| Overselling | **0/3** |
| Lost Update | **0/3** |
| ORDER history mismatch | **0/3** |
| HTTP/DB mismatch | **0/3** |
| Order -> History consistency | **PASS** |
| Order -> Inventory consistency | **PASS** |
| Conflict rollback consistency | **PASS** |

평균 235건의 `INVENTORY_CONFLICT`가 발생했지만 충돌 transaction은 rollback되어 committed Order, OrderItem, Inventory, InventoryHistory에 부분 데이터가 남은 징후가 없었다. `GenerationType.IDENTITY`에서 rollback된 transaction이 AUTO_INCREMENT 값을 소비해 발생하는 ID gap은 오류로 판정하지 않았고, 각 baseline 이후 실제 committed row `COUNT`로 검증했다.

`Unexpected409`, `UnexpectedHTTP`, `TransportErrors`는 3회 모두 0이므로 인프라 또는 클라이언트 오류가 정합성 결과에 영향을 준 정황은 없다.

### 7.6 충돌 감지와 utilization 해석

JPA Optimistic Lock은 `@Version` 값을 UPDATE 조건에 포함한다. 여러 transaction이 동일한 Inventory version을 읽더라도 먼저 UPDATE한 transaction만 성공하고, 이전 version으로 UPDATE하려는 나머지 transaction은 충돌로 감지된다. 이번 실험에서는 Run당 평균 235건, 전체 요청의 평균 23.5%가 `INVENTORY_CONFLICT`로 분류됐다.

충돌을 재시도하지 않았지만 3회 모두 `UtilizationPct`는 100%였고 최종 재고는 0이었다. 이는 충돌한 개별 요청이 다시 실행됐다는 뜻이 아니다. 1,000개의 독립 요청이 계속 유입되는 동안 앞선 충돌 이후 최신 version을 읽은 새로운 요청이 성공하면서 최종적으로 재고 100개가 소진된 결과다.

따라서 이번 3회의 결과만으로 `OPTIMISTIC_NO_RETRY`가 항상 100% utilization을 보장한다고 해석하지 않는다. 요청 수, 경합 수준, 도착 시점이 달라지면 충돌 이후 재고가 남을 수 있으므로 Correctness와 Utilization은 분리해 평가해야 한다.

### 7.7 성능 해석 주의

PowerShell 7에서 측정한 `ElapsedSec`, `AttemptedTPS`, `SuccessTPS`는 정합성 재현 과정의 reference benchmark다. 세 전략의 반복 횟수도 `NO_LOCK` 5회, `SYNCHRONIZED`와 `OPTIMISTIC_NO_RETRY` 각 3회로 동일하지 않으므로 이 수치만으로 절대적인 성능 우열이나 최종 동시성 전략을 확정하지 않는다.
