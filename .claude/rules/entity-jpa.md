---
paths:
  - "src/main/java/**"
---

# Entity / JPA / Repository

- Entity는 각 도메인의 `entity` 패키지에 둔다.
- 기본 생성자는 JPA용으로만 연다: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Entity를 API request/response에 직접 사용하지 않는다.
- 상태 변경은 setter 대신 의미 있는 메서드로 표현한다.
- 컬럼 제약은 `@Column(nullable = false, length = ...)`처럼 명시한다.
- Repository는 각 도메인의 `repository` 패키지에 interface로 둔다. 조회 결과가 없을 수 있으면 `Optional<T>`.
- `ddl-auto`: 로컬 `update`, 테스트 `create-drop`. 운영에서 `create`/`update`를 무분별하게 쓰지 않는다.
