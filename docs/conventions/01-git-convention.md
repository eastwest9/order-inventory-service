# Git Convention

## 1. 목적

본 문서는 Order Inventory Service 프로젝트의 Git 작업 방식을 표준화한다.

개인 프로젝트이지만 실제 협업 환경을 고려하여
Issue → Branch → Commit → Pull Request → CI → Merge 흐름을 따른다.

---

## 2. 기본 브랜치

기본 브랜치는 `main`을 사용한다.

### 원칙

- `main`은 항상 실행 및 테스트 가능한 상태를 유지한다.
- `main`에 직접 기능 개발을 하지 않는다.
- 기능 개발은 별도 작업 브랜치에서 수행한다.
- 작업 완료 후 Pull Request를 통해 `main`에 병합한다.
- 병합 완료 후 작업 브랜치는 삭제한다.

---

## 3. 브랜치 전략

GitHub Flow 기반의 간단한 브랜치 전략을 사용한다.

### 브랜치 형식

```text
<type>/<issue-number>-<description>