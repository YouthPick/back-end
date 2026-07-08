---
paths:
  - "src/main/java/**"
---

# Service 계층

- Service는 각 도메인의 `service` 패키지에 둔다. 비즈니스 판단, 중복 검사, 외부 API 호출, Entity 상태 변경은 Controller가 아니라 여기에 둔다.
- 읽기 메서드는 `@Transactional(readOnly = true)`, 생성/수정/삭제/외부 동기화는 명시적 `@Transactional`.
- 외부 API 호출, scheduler 등은 흐름을 명확히 나눈다.
- secret, API key, OAuth token 값은 로그에 남기지 않는다.

```java
@Transactional(readOnly = true)
public Page<PolicyCardResponse> search(Pageable pageable) {
    return repository.findAll(pageable).map(PolicyCardResponse::from);
}
```
