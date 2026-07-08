---
paths:
  - "src/main/java/**"
---

# 패키지 구조

도메인 우선 + 단순 구조를 유지한다. 패키지 루트는 `com.bop.youthpick`.

```text
com.bop.youthpick
├── global
│   ├── common      // ApiResponse 등 공통 응답 봉투
│   ├── config      // SecurityConfig, RestAuthenticationEntryPoint 등
│   └── error       // ErrorCode(인터페이스), CustomException, ErrorResponse, GlobalExceptionHandler
└── {domain}        // user, policy, auth, favorite(즐겨찾기), diagnosis(자가진단) ...
    ├── controller  // HTTP Controller와 API 입출력 조립
    ├── service     // 비즈니스 흐름, scheduler, lock, 외부 API client
    ├── repository  // Spring Data JPA repository interface
    ├── dto         // 요청/응답 DTO, service 결과 DTO, 외부 API payload DTO
    ├── entity      // JPA Entity, domain enum
    └── exception   // {Domain}ErrorCode(ErrorCode 구현 enum), {Domain}Exception(CustomException 상속)
```

- `global`에는 여러 도메인이 공유하는 공통 응답·설정·예외 처리만 둔다.
- 도메인에 아직 필요 없는 하위 패키지는 만들지 않는다.
- Controller는 얇게: HTTP 파싱, Bean Validation, Service 호출, 응답 DTO 변환만. 비즈니스 판단·중복 검사·외부 API 호출·Entity 상태 변경은 Service에 둔다.

**금지 구조** — DDD식 패키지를 만들지 않는다:

```text
com.bop.youthpick.{domain}
├── api / application / domain / infrastructure / external   // ❌
```
