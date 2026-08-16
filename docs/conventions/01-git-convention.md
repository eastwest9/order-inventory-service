# Git Convention

## 1. 목적

본 문서는 Order Inventory Service 프로젝트의 Git 작업 방식을 정의한다.

개인 프로젝트이지만 실제 협업 환경을 고려하여 다음 흐름으로 개발한다.

```text
Issue
→ Branch
→ Implementation
→ Test
→ Commit
→ Pull Request
→ CI
→ Squash and Merge
```

---

## 2. 기본 브랜치

기본 브랜치는 `main`을 사용한다.

### 규칙

- `main`은 항상 실행 가능한 상태를 유지한다.
- `main`에서 직접 기능 개발을 하지 않는다.
- `main`에 직접 Push하지 않는다.
- 작업은 별도 Branch에서 진행한다.
- Pull Request와 CI 확인 후 `main`에 병합한다.
- 병합된 작업 Branch는 삭제한다.

---

## 3. Branch Convention

GitHub Flow 기반으로 운영한다.

### 형식

```text
<type>/<issue-number>-<description>
```

### Type

| Type | 설명 |
|---|---|
| `feature` | 신규 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `test` | 테스트 |
| `perf` | 성능 개선 |
| `docs` | 문서 |
| `chore` | 설정 및 기타 작업 |
| `experiment` | 기술 실험 |
| `hotfix` | 긴급 수정 |

### 예시

```text
feature/10-product-create
feature/20-inventory-receipt
feature/30-order-create

fix/41-duplicate-order-cancel

test/50-order-concurrency

perf/60-atomic-stock-update

experiment/70-optimistic-lock-retry
```

### 규칙

- 영문 소문자를 사용한다.
- 단어 구분은 `-`를 사용한다.
- Issue 번호를 포함한다.
- 하나의 Branch는 하나의 목적만 가진다.

---

## 4. Commit Convention

Conventional Commits 형식을 사용한다.

### 형식

```text
<type>(<scope>): <subject>
```

### Type

| Type | 설명 |
|---|---|
| `feat` | 신규 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `perf` | 성능 개선 |
| `test` | 테스트 |
| `docs` | 문서 |
| `style` | 포맷 변경 |
| `build` | 빌드 / 의존성 |
| `ci` | CI/CD |
| `chore` | 기타 설정 |
| `revert` | 변경 되돌리기 |

### Scope

Phase 1:

```text
product
inventory
order
member
api
db
backend
frontend
common
load-test
infra
ci
docs
project
repo
```

Phase 확장 시 필요한 Scope를 추가한다.

예:

```text
auth
cart
payment
shipment
admin
```

### 작성 규칙

- `type`과 `scope`는 영문 소문자를 사용한다.
- `subject`는 한국어로 작성한다.
- 마침표를 사용하지 않는다.
- 하나의 Commit은 하나의 논리적인 변경을 기준으로 한다.
- 의미 없는 Commit Message를 사용하지 않는다.

### 좋은 예

```text
feat(order): 주문 생성 기능 추가
feat(inventory): 재고 입고 기능 추가
fix(order): 주문 취소 시 재고 중복 복구 방지
test(inventory): 비관적 락 동시성 테스트 추가
perf(inventory): 조건부 UPDATE 재고 차감 적용
docs(project): Phase 1 프로젝트 범위 정의
```

### 나쁜 예

```text
수정
작업
기능 추가
feat(order): 수정
```

---

## 5. Issue Convention

주요 작업은 GitHub Issue를 생성한 후 시작한다.

### 제목

```text
[DOMAIN] 작업 내용
```

### 예시

```text
[PRODUCT] 상품 등록 기능 구현
[INVENTORY] 재고 입고 기능 구현
[ORDER] 주문 생성 기능 구현
[TEST] 동시성 문제 재현
[PERF] 재고 차감 방식 성능 비교
```

### Issue에 포함할 내용

- 작업 목적
- 구현 범위
- 제외 범위
- 완료 조건
- 테스트 항목

---

## 6. Pull Request Convention

작업 완료 후 Pull Request를 생성한다.

### 제목

Commit Convention과 동일하게 작성한다.

```text
feat(order): 주문 생성 및 재고 차감 구현
```

### PR에 포함할 내용

- 관련 Issue
- 구현 내용
- 주요 설계 결정
- 테스트 결과
- DB 변경 여부
- API 변경 여부

---

## 7. Merge Convention

기본 Merge 방식은 다음을 사용한다.

```text
Squash and Merge
```

작업 Branch의 여러 Commit을 하나의 기능 단위 Commit으로 정리하여 `main`에 병합한다.

예:

작업 Branch:

```text
feat(order): 주문 요청 DTO 추가
feat(order): 주문 생성 서비스 구현
test(order): 주문 생성 테스트 추가
```

`main`:

```text
feat(order): 주문 생성 기능 구현 (#30)
```

---

## 8. Version / Release

Phase 완료 시 Git Tag와 GitHub Release를 생성한다.

```text
v0.1.0  Phase 1 - 상품 / 재고 / 주문 / 동시성
v0.2.0  Phase 2 - 회원 / 인증 / 권한
v0.3.0  Phase 3 - 장바구니 / 관리자
v0.4.0  Phase 4 - 결제
v0.5.0  Phase 5 - 배송 / 송장
v1.0.0  최종 안정 버전
```

버그 수정은 Patch 버전을 증가시킨다.

```text
v0.1.0
→ v0.1.1
```

---

## 9. 금지 사항

- `main` 직접 Push
- `main` Force Push
- 테스트 실패 상태에서 Merge
- `.env`, 비밀번호, API Key 등 Secret Commit
- 서로 관계없는 변경을 하나의 Commit에 포함
- 검토하지 않은 AI 생성 코드를 그대로 Commit
- Release 이후 기존 Flyway Migration 파일 수정

---

## 10. 기본 Workflow

```text
Issue 생성
↓
main 최신화
↓
Branch 생성
↓
구현
↓
Test
↓
Commit
↓
Push
↓
Pull Request
↓
CI
↓
Squash and Merge
↓
Branch 삭제
```