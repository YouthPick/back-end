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
├── admin           // 관리자 전용 코드 — 도메인별 하위 패키지로만 구성
│   └── {domain}    // admin.policy, admin.board, admin.log, admin.sync, admin.user ...
│       ├── controller  // Admin* 컨트롤러 (/api/v1/admin/** 경로)
│       ├── service     // Admin* 서비스
│       ├── repository  // Admin*Specifications 등 admin 전용 query-spec
│       └── dto         // admin 응답/요청 전용 DTO
└── {domain}        // user, policy, auth, favorite(즐겨찾기), diagnosis(자가진단) ...
    ├── controller  // HTTP Controller와 API 입출력 조립
    ├── service     // 비즈니스 흐름, scheduler, lock, 외부 API client
    ├── repository  // Spring Data JPA repository interface
    ├── dto         // 요청/응답 DTO, service 결과 DTO, 외부 API payload DTO
    ├── entity      // JPA Entity, domain enum
    └── exception   // {Domain}ErrorCode(ErrorCode 구현 enum), {Domain}Exception(CustomException 상속)
```

- `global`에는 여러 도메인이 공유하는 공통 응답·설정·예외 처리만 둔다.
- 관리자(Admin) 전용 컨트롤러/서비스/query-spec/전용 DTO는 각 도메인이 아니라 최상위 `admin.{domain}` 패키지에 둔다(예: `admin.policy.controller.AdminPolicyController`). 여러 도메인에서 공유하는 Entity·일반 Repository·`{Domain}ErrorCode`는 그대로 각 도메인 패키지에 남기고, admin 쪽에서 그 도메인 패키지를 참조한다 — 반대 방향(도메인 → admin) 의존은 만들지 않는다.
- 도메인에 아직 필요 없는 하위 패키지는 만들지 않는다.
- Controller는 얇게: HTTP 파싱, Bean Validation, Service 호출, 응답 DTO 변환만. 비즈니스 판단·중복 검사·외부 API 호출·Entity 상태 변경은 Service에 둔다.

**금지 구조** — DDD식 패키지를 만들지 않는다:

```text
com.bop.youthpick.{domain}
├── api / application / domain / infrastructure / external   // ❌
```
