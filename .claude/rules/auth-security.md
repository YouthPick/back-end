---
paths:
  - "src/main/java/**"
  - "src/main/resources/**"
  - ".env.example"
---

# 인증 / JWT / Redis / Secret

Spring Security **STATELESS** + **JWT(access/refresh) 인증** + OAuth 소셜 로그인(Google / Naver / Kakao) 구조다. 서버 세션(`HttpSession`)은 쓰지 않는다. refresh token만 예외적으로 HttpOnly 쿠키를 쓴다(아래 참고) — 그 외 새 세션/쿠키 기반 인증을 추가하지 않는다.

- access token 인증은 `Authorization: Bearer <accessToken>` 헤더로 이루어진다. `JwtAuthenticationFilter`가 매 요청에서 토큰을 검증해 `SecurityContext`를 채우고, `SecurityConfig`는 `SessionCreationPolicy.STATELESS`를 유지한다.
- refresh token은 **HttpOnly 쿠키**(`RefreshTokenCookieSupport`, 쿠키명 `RefreshTokenCookieSupport.COOKIE_NAME`)로만 클라이언트에 전달한다. JS가 `document.cookie`로 읽을 수 없게 해 XSS로부터 보호하기 위함이다 — 요청 body/응답 body 어디에도 refresh token 원문을 담지 않는다(`AuthController`는 `AccessTokenResponse`로 accessToken만 body에 내려준다). 쿠키 속성(`secure`/`sameSite`/`path`)은 `RefreshCookieProperties`(`youthpick.auth.refresh-cookie.*`)로 프로필별 관리한다 — 프론트와 백엔드가 크로스사이트인 배포 환경은 `SameSite=None; Secure` 필수, `localhost` 로컬 개발은 `SameSite=Lax`로 완화한다.
- refresh token은 발급 시 `RefreshTokenStore`(Redis, `youthpick.jwt.refresh-token-expiration` TTL)에 사용자별로 저장한다(쿠키는 전달 경로일 뿐이고, 유효성의 근원은 여전히 Redis다). `/api/v1/auth/token/refresh`는 쿠키에서 꺼낸 토큰의 JWT 서명·만료를 검증한 뒤 Redis에 저장된 값과 일치하는지(`RefreshTokenStore.matches`) 확인하고, 통과하면 access token만 새로 발급한다 — refresh token은 재발급(rotate)하지 않고 최초 로그인 시점의 TTL이 다할 때까지 그대로 재사용하므로 쿠키도 다시 내려주지 않는다. 로그아웃은 Redis의 refresh token을 삭제하고 쿠키도 함께 지운다(`RefreshTokenCookieSupport.clear()`).
- refresh 엔드포인트는 쿠키만으로 동작해 preflight 없는 단순 요청도 도달할 수 있으므로, `AuthController.refresh()`는 `Origin` 헤더가 있는데 `SecurityConfig.ALLOWED_ORIGINS`에 없으면 서비스 호출 전에 거부한다(CSRF 최소 방어). 새로 쿠키를 소비하는 엔드포인트를 추가하면 동일한 가드를 적용할지 검토한다.
- OAuth authorization-url 발급 시 CSRF 방지용 `state`는 세션이 아니라 `OAuthStateStore`(Redis, TTL)에 저장하고 콜백에서 1회 소비한다.
- 인증 실패(미인증 접근)는 리다이렉트가 아니라 `RestAuthenticationEntryPoint`에서 JSON + `A001 UNAUTHORIZED`로 내려준다.
- 인가 규칙(경로별 권한)은 `SecurityConfig`의 `authorizeHttpRequests`에서 경로 매처로 관리하고 Controller에 흩뿌리지 않는다. 새 엔드포인트를 인증 필수로 만들려면 이 파일의 `requestMatchers(...).authenticated()` 목록에 경로를 추가한다.
- 로그인한 사용자의 id가 필요하면 컨트롤러 파라미터에 `@CurrentUser Long userId`를 붙인다. `CurrentUserArgumentResolver`가 `SecurityContext`의 `AuthPrincipal.userId()`를 그대로 주입한다(DB 조회 없음). 인증되지 않았으면 `AuthException(UNAUTHORIZED)`를 던진다. `User` 엔티티 자체가 필요하면 컨트롤러에서 `authService.getCurrentUser(userId)`를 호출한다(Controller→Service 호출 규칙을 그대로 따름).
- OAuth client id/secret, JWT secret 등 secret 값은 하드코딩하지 않고 환경변수(`GOOGLE_OAUTH_CLIENT_ID`, `JWT_SECRET` 등)로 주입한다. 새 환경변수는 `.env.example`에 placeholder로 추가한다.
- secret / token 값을 코드·로그·응답 어디에도 노출하지 않는다. `.env`는 커밋하지 않는다.
- 중복 실행을 막아야 하는 배치/스케줄 작업을 도입하면 Redis lock(owner token + TTL, release 시 owner 확인)을 사용하고, scheduler는 설정으로 on/off 가능하게 둔다.
