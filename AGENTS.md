# Order Inventory Service - Agent Instructions

## 1. Project Goal

이 프로젝트는 주문·재고 시스템을 기반으로
대량 동시 주문 환경에서 재고 정합성과 동시성 제어 전략을 검증하는 프로젝트다.

현재 개발 단계는 Phase 1이다.

상세 범위와 업무 규칙은 다음 문서를 따른다.

- docs/phase1/01-project-charter.md
- docs/phase1/02-business-rules.md

---

## 2. Phase 1 Scope

현재 구현 대상:

- product
- product variant / SKU
- inventory
- inventory history
- order
- order item
- minimal member

현재 구현 금지:

- authentication
- authorization
- cart
- payment
- shipment
- Redis
- Kafka
- microservices

Phase 1 범위를 임의로 확장하지 않는다.

---

## 3. Technology

Backend:

- Java 21
- Spring Boot
- Gradle
- Spring Data JPA
- MySQL
- Flyway

Frontend:

- React
- TypeScript
- TanStack Query

Testing:

- JUnit 5
- Spring Boot Test
- Testcontainers
- k6

Infrastructure:

- Docker
- Docker Compose
- GitHub Actions

---

## 4. Architecture Rules

Package by domain을 사용한다.

주요 도메인:

- product
- inventory
- order
- member
- common

Controller:

- HTTP 요청/응답 처리만 담당한다.
- 비즈니스 로직을 작성하지 않는다.
- Repository를 직접 호출하지 않는다.

Service/Application:

- Use Case를 조정한다.
- 트랜잭션 경계를 관리한다.

Domain:

- 핵심 업무 규칙을 담당한다.

Repository:

- 데이터 접근을 담당한다.
- 업무 규칙을 작성하지 않는다.

---

## 5. Database Rules

- DB 스키마는 Flyway로 관리한다.
- 이미 main에 반영된 migration 파일은 수정하지 않는다.
- 새로운 DB 변경은 새로운 migration 파일로 추가한다.
- PK와 FK를 명확하게 정의한다.
- 금액에 float/double을 사용하지 않는다.
- 재고 수량은 음수가 될 수 없다.
- 모든 재고 변경은 inventory_history에 기록한다.

---

## 6. Transaction Rules

- 주문 생성과 재고 차감은 하나의 트랜잭션으로 처리한다.
- 주문 실패 시 부분 데이터가 남아서는 안 된다.
- 주문 취소와 재고 복구는 하나의 트랜잭션으로 처리한다.
- Controller에 @Transactional을 사용하지 않는다.

---

## 7. Concurrency Rules

Phase 1의 핵심 목적은 동시성 문제를 직접 재현하고 비교하는 것이다.

따라서:

- 처음부터 Lock을 적용하지 않는다.
- Race Condition 재현 테스트를 먼저 작성한다.
- 동시성 전략별로 업무 규칙을 변경하지 않는다.
- 테스트 조건을 동일하게 유지한다.
- 테스트 결과 없이 최종 전략을 선택하지 않는다.

비교 대상:

- No Lock
- synchronized
- Optimistic Lock
- Pessimistic Lock
- Conditional Atomic Update

---

## 8. Testing Rules

테스트 유형을 구분한다.

- Unit Test
- Repository Test
- Integration Test
- Concurrency Test
- Load Test

테스트는 실행 순서에 의존하지 않는다.

가능하면 Given / When / Then 구조를 사용한다.

동시성 테스트에서는 최소한 다음 값을 검증한다.

- 성공 주문 수
- 실패 주문 수
- 최종 재고
- 성공 주문 수량 합계
- 재고 차감 이력 합계

---

## 9. Git Rules

Workflow:

Issue
→ Branch
→ Implementation
→ Test
→ Pull Request
→ CI
→ Squash and Merge

main 브랜치에 직접 push하지 않는다.

Commit format:

type(scope): 한국어 설명

Examples:

feat(order): 주문 생성 기능 추가
feat(inventory): 재고 입고 기능 추가
test(inventory): 비관적 락 동시성 테스트 추가
perf(inventory): 원자적 재고 차감 쿼리 적용

---

## 10. Agent Behavior

작업 요청을 받았을 때:

1. 관련 문서를 먼저 확인한다.
2. 현재 코드 구조를 분석한다.
3. 구현 범위를 확인한다.
4. 필요한 경우 구현 계획을 먼저 제안한다.
5. 요청받은 범위를 넘어서는 기능을 임의로 추가하지 않는다.
6. 기존 비즈니스 규칙을 임의로 변경하지 않는다.
7. 변경 후 관련 테스트를 실행한다.
8. 테스트 실패를 숨기지 않는다.
9. 변경된 파일과 주요 설계 결정을 요약한다.
10. 명시적으로 요청받지 않은 경우 Git commit이나 push를 실행하지 않는다.

특히 동시성 코드에서는 문제를 임의로 해결하지 말고
현재 실험 단계와 목적을 먼저 확인한다.