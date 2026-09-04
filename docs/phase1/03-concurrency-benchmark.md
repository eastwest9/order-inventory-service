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
| PESSIMISTIC_LOCK | PASS (3/3) | 0/3 | 0/3 | 100 | 12.514 | 81.83 | 8.18 | 0건 | DB row lock, 충돌 시 대기 |
| CONDITIONAL_ATOMIC_UPDATE | PASS (3/3) | 0/3 | 0/3 | 100 | 10.812 | 92.63 | 9.26 | 0건 | 조건 검사와 차감을 하나의 UPDATE로 처리 |

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

## 8. PESSIMISTIC_LOCK

### 8.1 구현 구조

Inventory entity에서 JPA Optimistic Lock에 사용했던 `@Version` annotation과 `version` 필드를 제거했다. DB의 `inventory.version` 컬럼은 그대로 존재하지만 이번 전략에서는 JPA가 관리하지 않으며 correctness 판정 기준으로 사용하지 않았다.

InventoryRepository에는 주문 생성 전용 조회를 추가했다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
        select i
        from Inventory i
        where i.productVariant.id = :variantId
        """)
Optional<Inventory> findByProductVariantIdForUpdate(
        @Param("variantId") Long variantId
);
```

이 조회는 개념적으로 MySQL에서 `SELECT ... FOR UPDATE` 형태의 row lock을 획득한다. 기존 일반 조회인 `findByProductVariant_Id(...)`는 별도의 Pessimistic Lock 없이 그대로 유지했다.

활성 주문 생성 경로는 다음과 같다.

```text
OrderController
-> OrderService.createOrder() @Transactional
-> InventoryRepository.findByProductVariantIdForUpdate()
-> Inventory row lock 획득
-> quantity 확인 및 decrease()
-> ORDER history 저장
-> commit
-> row lock 해제
```

`createOrder()`의 기존 트랜잭션 경계를 유지했으며 Retry와 Conditional Atomic Update는 적용하지 않았다. `SynchronizedOrderService`는 코드에 남아 있지만 active path가 아니고, Optimistic `@Version`도 활성화되어 있지 않다.

### 8.2 실험 조건

| 항목 | 값 |
|---|---:|
| Strategy | `PESSIMISTIC_LOCK` |
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

`NO_LOCK`, `SYNCHRONIZED`, `OPTIMISTIC_NO_RETRY`와 동일한 SKU 및 부하 조건을 유지했다. 시간 제약으로 이 전략도 3회 반복 검증했다.

### 8.3 실행 결과

#### 8.3.1 HTTP 결과

| Run | HTTP201 | InsufficientInventory409 | InventoryConflict409 | Unexpected409 | UnexpectedHTTP | TransportErrors | ConflictRatePct | ElapsedSec | AttemptedTPS | SuccessTPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 900 | 0 | 0 | 0 | 0 | 0% | 15.308 | 65.32 | 6.53 |
| RUN-02 | 100 | 900 | 0 | 0 | 0 | 0 | 0% | 10.682 | 93.62 | 9.36 |
| RUN-03 | 100 | 900 | 0 | 0 | 0 | 0 | 0% | 11.553 | 86.56 | 8.66 |

각 Run에서 `HTTP201 + InsufficientInventory409 + InventoryConflict409 + Unexpected409 + UnexpectedHTTP + TransportErrors = 1,000`이었다.

#### 8.3.2 Inventory 결과

| Run | InitialQty | FinalQty | ActualDecrease | DbVersion (informational) | UtilizationPct |
|---|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 0 | 100 | 303 | 100% |
| RUN-02 | 100 | 0 | 100 | 303 | 100% |
| RUN-03 | 100 | 0 | 100 | 303 | 100% |

DB의 `inventory.version` 값은 세 Run에서 303으로 유지됐다. Inventory entity에 `@Version` 매핑이 없으므로 이 값은 informational data로만 기록했으며 correctness 판정에는 사용하지 않았다.

#### 8.3.3 DB 정합성 결과

| Run | CommittedOrders | OrderItems | SuccessQty | HistoryCount | SignedHistoryChange | HistoryDecrease | MaxSameTransition | UniqueTransitions | DuplicateTransition | InvalidTransition | Correctness | Oversold | LostUpdate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---:|---|---|---|
| RUN-01 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | 0 | PASS | false | false |
| RUN-02 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | 0 | PASS | false | false |
| RUN-03 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | 0 | PASS | false | false |

세 Run 모두 CANCELED 주문과 `ORDER_CANCEL` history는 0건이었다. Committed `ORDER` history는 `100 -> 99`부터 `1 -> 0`까지 각 transition이 한 번씩 존재했고, 중복 및 비정상 transition은 없었다.

각 Run의 900건 `INSUFFICIENT_INVENTORY` 실패 요청에 대해서도 baseline 이후 실제 committed row를 집계했다. 세 Run 모두 HTTP 성공 수와 Order, OrderItem, ORDER history 수가 100으로 일치해 실패 요청이 partial commit을 남긴 징후는 없었다. AUTO_INCREMENT ID gap은 건수로 사용하거나 오류로 판정하지 않았다.

### 8.4 3회 집계

| 지표 | 최소 | 최대 | 평균 |
|---|---:|---:|---:|
| HTTP201 | 100 | 100 | 100 |
| InsufficientInventory409 | 900 | 900 | 900 |
| InventoryConflict409 | 0 | 0 | 0 |
| ElapsedSec | 10.682 | 15.308 | 12.514 |
| AttemptedTPS | 65.32 | 93.62 | 81.83 |
| SuccessTPS | 6.53 | 9.36 | 8.18 |

- Correctness: **PASS (3/3)**
- Overselling: **0/3**
- Lost Update: **0/3**
- ORDER history mismatch: **0/3**
- HTTP/DB mismatch: **0/3**
- Utilization 100%: **3/3**
- InventoryConflict409: **총 0건**
- Unexpected409: **총 0건**
- UnexpectedHTTP: **총 0건**
- TransportErrors: **총 0건**
- Lock timeout/deadlock: **0/3 관찰**

### 8.5 정합성 판정

3회 모두 다음 관계가 성립했다.

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
| ORDER history mismatch | **0/3** |
| HTTP/DB mismatch | **0/3** |
| Order -> History consistency | **PASS** |
| Order -> Inventory consistency | **PASS** |
| Failure rollback consistency | **PASS** |

세 Run 모두 성공한 주문, 실제 Inventory 감소량, ORDER history 감소량이 일치했다. 따라서 초과 판매, Lost Update, ORDER history mismatch, HTTP/DB mismatch는 관찰되지 않았다.

### 8.6 Row lock 동작과 Optimistic Lock 비교

`PESSIMISTIC_WRITE`는 동일한 Inventory row를 변경하려는 transaction을 DB row lock으로 직렬화한다. 먼저 lock을 획득한 transaction이 최신 quantity를 읽고 감소한 뒤 commit하면 lock이 해제되고, 다음 transaction이 lock을 획득해 최신 committed quantity를 읽는다.

이번 실험에서는 이 순서로 재고 100개에 대한 주문만 성공했다. 이후 요청 900개는 lock 획득 후 최신 `quantity=0`을 확인해 `INSUFFICIENT_INVENTORY`로 종료됐다. 이에 따라 세 Run 모두 `HTTP201 = CommittedOrderCount = OrderItemCount = ActualInventoryDecrease = HistoryDecrease = 100` 관계가 유지됐다.

`OPTIMISTIC_NO_RETRY`는 `@Version`으로 충돌을 감지하고 충돌 요청을 즉시 실패시켜 평균 235건, 23.5%의 `INVENTORY_CONFLICT`가 발생했다. 반면 `PESSIMISTIC_LOCK`은 DB row lock 획득을 기다린 뒤 처리했기 때문에 이번 실험의 `INVENTORY_CONFLICT`는 총 0건이었다. 성공 가능한 요청은 lock 획득 순서대로 처리됐고 최종 결과는 성공 100건, 재고 부족 900건이었다.

Pessimistic Lock에는 lock wait 증가와 lock timeout 가능성이 있다. 여러 SKU를 한 트랜잭션에서 서로 다른 순서로 잠그면 deadlock이 발생할 수도 있다. 이번 단일 SKU 실험에서는 세 Run 모두 lock timeout이나 deadlock이 관찰되지 않았지만, 일반적으로 이러한 위험이 없다고 확대 해석하지 않는다.

### 8.7 성능 해석 주의

PowerShell 7에서 측정한 `ElapsedSec`, `AttemptedTPS`, `SuccessTPS`는 동일 로컬 환경에서 수행한 정합성 재현 목적의 reference benchmark다. 이번 결과만으로 Pessimistic Lock이 Optimistic Lock보다 항상 빠르거나 특정 전략이 최종적으로 가장 높은 성능을 낸다고 단정하지 않는다.

네 전략의 반복 횟수, 로컬 실행 환경, PowerShell 기반 client 부하 생성 및 실행 시점의 환경 변동을 고려해야 한다. 최종 전략 선택은 이후 Conditional Atomic Update까지 동일한 조건으로 측정한 결과와 운영 환경의 lock wait, timeout, deadlock 특성을 함께 비교해 판단한다.

## 9. CONDITIONAL_ATOMIC_UPDATE

### 9.1 구현 구조

활성 주문 생성 경로는 다음과 같다.

```text
POST /api/orders
-> OrderController.createOrder()
-> OrderService.createOrder() @Transactional
-> InventoryRepository.decreaseQuantityIfAvailable()
-> 조건부 atomic UPDATE
-> 일반 Inventory 조회
-> InventoryHistory.order() 저장
```

이번 전략에서는 Conditional Atomic Update가 active path이며 Pessimistic Lock, Optimistic `@Version`, `synchronized`, Retry는 활성화하지 않았다.

`InventoryRepository.decreaseQuantityIfAvailable()`의 실제 JPQL은 `Inventory` entity의 `quantity`와 `productVariant.id` 매핑을 사용한다.

```java
@Modifying
@Query("""
        update Inventory i
        set i.quantity = i.quantity - :quantity
        where i.productVariant.id = :variantId
          and i.quantity >= :quantity
        """)
int decreaseQuantityIfAvailable(
        @Param("variantId") Long variantId,
        @Param("quantity") int quantity
);
```

의미상 재고 확인, 수량 조건 검사, 차감을 하나의 조건부 UPDATE에서 수행한다. updated row count가 1이면 차감 성공이고, 0이면 요청 수량 이상이라는 조건을 충족하지 못한 것으로 보고 `INSUFFICIENT_INVENTORY`로 처리한다. 성공 후에는 일반 Inventory 조회 결과로 차감 전후 수량을 계산해 `InventoryHistory.order()`를 저장한다.

이 방식은 애플리케이션의 read-modify-write 경쟁과 별도의 `SELECT FOR UPDATE`를 피하지만 lock-free는 아니다. MySQL/InnoDB가 UPDATE를 처리하는 과정에서는 row lock과 lock wait가 발생할 수 있다.

### 9.2 실험 조건

| 항목 | 값 |
|---|---:|
| Strategy | `CONDITIONAL_ATOMIC_UPDATE` |
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

앞선 전략과 동일한 SKU 및 부하 조건을 유지했고 3회 반복 검증했다.

### 9.3 실행 결과

#### 9.3.1 HTTP 결과

| Run | HTTP201 | InsufficientInventory409 | InventoryConflict409 | Unexpected409 | UnexpectedHTTP | TransportErrors | ElapsedSec | AttemptedTPS | SuccessTPS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 900 | 0 | 0 | 0 | 0 | 11.295 | 88.54 | 8.85 |
| RUN-02 | 100 | 900 | 0 | 0 | 0 | 0 | 10.277 | 97.31 | 9.73 |
| RUN-03 | 100 | 900 | 0 | 0 | 0 | 0 | 10.865 | 92.04 | 9.20 |

각 Run에서 `HTTP201 + InsufficientInventory409 + InventoryConflict409 + Unexpected409 + UnexpectedHTTP + TransportErrors = 1,000`이었다.

#### 9.3.2 Inventory 결과

| Run | InitialQty | FinalQty | ActualDecrease | DbVersion (informational) | UtilizationPct |
|---|---:|---:|---:|---:|---:|
| RUN-01 | 100 | 0 | 100 | 303 | 100% |
| RUN-02 | 100 | 0 | 100 | 303 | 100% |
| RUN-03 | 100 | 0 | 100 | 303 | 100% |

DB의 Inventory version 값은 세 Run에서 303으로 유지됐다. 현재 전략에서는 Inventory entity의 `@Version`이 비활성이므로 이 값은 informational data로만 기록했으며 correctness 판정에는 사용하지 않았다.

#### 9.3.3 DB 정합성 결과

| Run | CommittedOrders | OrderItems | SuccessQty | HistoryCount | SignedHistoryChange | HistoryDecrease | MaxSameTransition | UniqueTransitions | DuplicateTransition | Correctness | Oversold | LostUpdate | OrderHistoryMismatch | HttpDbMismatch |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|---|---|---|
| RUN-01 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | PASS | false | false | false | false |
| RUN-02 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | PASS | false | false | false | false |
| RUN-03 | 100 | 100 | 100 | 100 | -100 | 100 | 1 | 100 | false | PASS | false | false | false | false |

세 Run 모두 committed `ORDER` history는 `100 -> 99`부터 `1 -> 0`까지 각 transition이 한 번씩 존재했고 중복 transition은 없었다. HTTP 성공 수와 Order, OrderItem, 실제 Inventory 감소량, ORDER history 감소량도 모두 100으로 일치했다.

### 9.4 3회 집계

| 지표 | 최소 | 최대 | 평균 |
|---|---:|---:|---:|
| HTTP201 | 100 | 100 | 100 |
| InsufficientInventory409 | 900 | 900 | 900 |
| InventoryConflict409 | 0 | 0 | 0 |
| ElapsedSec | 10.277 | 11.295 | 10.812 |
| AttemptedTPS | 88.54 | 97.31 | 92.63 |
| SuccessTPS | 8.85 | 9.73 | 9.26 |

- Correctness: **PASS (3/3)**
- Overselling: **0/3**
- Lost Update: **0/3**
- Inventory conflict: **0/3**
- Unexpected409: **총 0건**
- UnexpectedHTTP: **총 0건**
- TransportErrors: **총 0건**

### 9.5 정합성 판정

3회 모두 다음 관계가 성립했다.

```text
HTTP201
= CommittedOrderCount
= OrderItemCount
= SuccessQuantitySum
= ActualInventoryDecrease
= HistoryDecrease
= 100
```

| 평가 항목 | 판정 |
|---|---|
| Correctness | **PASS (3/3)** |
| Overselling | **0/3** |
| Lost Update | **0/3** |
| Inventory conflict | **0/3** |
| ORDER history mismatch | **0/3** |
| HTTP/DB mismatch | **0/3** |
| Order -> History consistency | **PASS** |
| Order -> Inventory consistency | **PASS** |

updated row count를 성공 여부로 사용함으로써 동일 재고를 읽은 뒤 각자 계산하고 덮어쓰는 흐름이 발생하지 않았다. 세 Run 모두 초과 판매, Lost Update, ORDER history mismatch, HTTP/DB mismatch가 관찰되지 않았다.

### 9.6 한계와 후속 검증

- Conditional Atomic Update도 lock-free는 아니며 InnoDB UPDATE 과정에서 row lock 대기가 발생할 수 있다.
- 여러 SKU를 하나의 주문에서 서로 다른 순서로 갱신하면 deadlock이 발생할 수 있다.
- 복잡한 재고 불변식이나 여러 row의 상태를 함께 검증해야 한다면 `PESSIMISTIC_LOCK` 같은 전략이 더 적절할 수 있다.
- deadlock 및 retry 정책은 현재 Issue #11의 단일 SKU benchmark 범위 밖이다.
- `updatedRows == 0` 이후 available quantity를 조회하는 시점과 transaction isolation에 따른 예외 payload 정확성은 후속 Testcontainers/MySQL 통합 테스트에서 추가 검증할 수 있다.

### 9.7 성능 해석 주의

PowerShell 7에서 측정한 `ElapsedSec`, `AttemptedTPS`, `SuccessTPS`는 정밀 성능 벤치마크가 아니라 동일 로컬 환경에서 전략 간 결과를 비교하기 위한 reference benchmark다. 이번 측정에서 정합성을 유지한 전략 중 가장 짧은 평균 elapsed와 가장 높은 attempted/success TPS를 기록했지만, 반복 횟수, client 부하 생성 방식, 실행 시점의 환경 변동과 운영 환경의 DB lock 특성이 다르므로 절대적인 성능 우위로 일반화하지 않는다.

## 10. 최종 비교 및 선택

### 10.1 전체 전략 비교

| Strategy | Correctness | Overselling / Lost Update | ElapsedSec Avg | AttemptedTPS Avg | SuccessTPS Avg | Conflict | 동시성 제어 특성 |
|---|---|---|---:|---:|---:|---|---|
| NO_LOCK | FAIL | 발생 | 16.709 | 63.29 | 미기록 | 해당 없음 | read-modify-write 경쟁으로 정합성 훼손 |
| SYNCHRONIZED | PASS (3/3) | 0/3 | 21.903 | 46.08 | 4.61 | 없음 | 단일 JVM monitor에 의존 |
| OPTIMISTIC_NO_RETRY | PASS (3/3) | 0/3 | 13.899 | 73.47 | 7.35 | 평균 23.5% | version conflict를 감지하고 retry 없이 실패 처리 |
| PESSIMISTIC_LOCK | PASS (3/3) | 0/3 | 12.514 | 81.83 | 8.18 | 없음 | `SELECT FOR UPDATE` 후 재고 확인 및 변경 |
| CONDITIONAL_ATOMIC_UPDATE | PASS (3/3) | 0/3 | 10.812 | 92.63 | 9.26 | 없음 | 조건 검사와 차감을 하나의 UPDATE로 처리 |

### 10.2 최종 선택

Issue #11의 최종 전략은 `CONDITIONAL_ATOMIC_UPDATE`로 선택한다.

현재 Phase 1의 핵심 재고 불변식은 특정 SKU의 재고가 요청 수량 이상일 때 차감하는 비교적 단순한 조건이다. Conditional Atomic Update는 재고 확인, 조건 검사, 차감을 하나의 DB UPDATE에서 수행해 애플리케이션 레벨 read-modify-write 경쟁을 제거한다. 이에 따라 `NO_LOCK`에서 나타난 Lost Update가 발생하지 않았다.

또한 단일 JVM lock에 의존하는 `SYNCHRONIZED`와 달리 다중 application instance에서도 동일한 DB 정합성 방식이 적용된다. `OPTIMISTIC_NO_RETRY`에서 높은 contention 중 나타난 애플리케이션 수준의 version conflict도 이번 전략에서는 발생하지 않았다. `PESSIMISTIC_LOCK`의 `SELECT FOR UPDATE` 후 수정 흐름과 비교하면, 현재의 단순한 불변식은 조건부 UPDATE 하나로 처리할 수 있어 구조가 더 단순하다.

이번 로컬 reference benchmark에서는 정합성을 유지한 전략 중 평균 elapsed가 가장 짧고 attempted/success TPS가 가장 높았다. 그러나 TPS가 가장 높다는 이유만으로 선택한 것은 아니며, 이 결과를 절대적인 성능 우위로 일반화하지 않는다. 현재와 같이 단순한 재고 조건부 차감이 핵심인 요구사항에서는 DB의 조건부 원자 UPDATE를 이용해 정합성을 보장하는 방식이 구현 복잡도와 동시성 제어 측면에서 가장 적합하다고 판단했다.

다만 요구사항이 여러 row의 상태를 함께 잠근 뒤 검증해야 하는 형태로 복잡해지면 `PESSIMISTIC_LOCK`이 더 적절할 수 있다. Conditional Atomic Update 역시 InnoDB row lock 대기와 다중 SKU 갱신 순서에 따른 deadlock 가능성이 있으므로, 운영 환경 적용 전 lock wait와 deadlock 및 retry 정책을 별도로 검증해야 한다.
