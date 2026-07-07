---
paths:
  - "src/test/**"
---

# 테스트

- 기능 변경 시 관련 작은 테스트를 먼저 실행하고, 마지막에 전체 테스트(`./gradlew test`)를 실행한다.
- Controller validation은 MockMvc 테스트로 검증한다.
- Service 비즈니스 규칙은 Service 테스트로 검증한다.
- Repository/JPA 쿼리는 통합 테스트로 검증한다(테스트 프로파일은 H2 `create-drop`).
- A(정상) / E(예외) / X(경계·비정상) 관점을 모두 다룬다.
