---
paths:
  - "src/main/java/**"
  - "src/main/resources/**"
  - ".env.example"
---

# 인증 / 세션 / Redis / Secret

Spring Security **세션 기반 인증** + Spring Session(**Redis 저장소**) + OAuth 소셜 로그인(Google / Naver / Kakao) 구조다.

- 세션은 Spring Session을 통해 Redis에 저장한다(`spring-session-data-redis`). in-memory 세션에 의존하는 코드를 만들지 않는다.
- 인증 실패(미인증 접근)는 리다이렉트가 아니라 `RestAuthenticationEntryPoint`에서 JSON + `A001 UNAUTHORIZED`로 내려준다.
- 인가 규칙(경로별 권한)은 `SecurityConfig`에서 관리하고 Controller에 흩뿌리지 않는다.
- OAuth client id/secret 등 secret 값은 하드코딩하지 않고 환경변수(`GOOGLE_OAUTH_CLIENT_ID` 등)로 주입한다. 새 환경변수는 `.env.example`에 placeholder로 추가한다.
- secret / token 값을 코드·로그·응답 어디에도 노출하지 않는다. `.env`는 커밋하지 않는다.
- 중복 실행을 막아야 하는 배치/스케줄 작업을 도입하면 Redis lock(owner token + TTL, release 시 owner 확인)을 사용하고, scheduler는 설정으로 on/off 가능하게 둔다.
