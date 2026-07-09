---
paths:
  - "src/main/java/**"
  - "src/main/resources/**"
  - ".env.example"
---

# 인증 / JWT / Redis / Secret

Spring Security **STATELESS** + **JWT(access/refresh) 인증** + OAuth 소셜 로그인(Google / Naver / Kakao) 구조다. 세션/쿠키는 쓰지 않는다.

- 인증은 `Authorization: Bearer <accessToken>` 헤더로 이루어진다. `JwtAuthenticationFilter`가 매 요청에서 토큰을 검증해 `SecurityContext`를 채우고, `SecurityConfig`는 `SessionCreationPolicy.STATELESS`를 유지한다. 세션/쿠키 기반 인증 코드를 다시 만들지 않는다.
- refresh token은 발급 시 `RefreshTokenStore`(Redis, `youthpick.jwt.refresh-token-expiration` TTL)에 사용자별로 저장한다. `/api/v1/auth/token/refresh`는 JWT 서명·만료를 검증한 뒤 Redis에 저장된 값과 일치하는지 확인하고, 통과하면 access/refresh를 모두 재발급(rotate)한다. 로그아웃은 Redis의 refresh token을 삭제한다.
- OAuth authorization-url 발급 시 CSRF 방지용 `state`는 세션이 아니라 `OAuthStateStore`(Redis, TTL)에 저장하고 콜백에서 1회 소비한다.
- 인증 실패(미인증 접근)는 리다이렉트가 아니라 `RestAuthenticationEntryPoint`에서 JSON + `A001 UNAUTHORIZED`로 내려준다.
- 인가 규칙(경로별 권한)은 `SecurityConfig`에서 관리하고 Controller에 흩뿌리지 않는다.
- OAuth client id/secret, JWT secret 등 secret 값은 하드코딩하지 않고 환경변수(`GOOGLE_OAUTH_CLIENT_ID`, `JWT_SECRET` 등)로 주입한다. 새 환경변수는 `.env.example`에 placeholder로 추가한다.
- secret / token 값을 코드·로그·응답 어디에도 노출하지 않는다. `.env`는 커밋하지 않는다.
- 중복 실행을 막아야 하는 배치/스케줄 작업을 도입하면 Redis lock(owner token + TTL, release 시 owner 확인)을 사용하고, scheduler는 설정으로 on/off 가능하게 둔다.
