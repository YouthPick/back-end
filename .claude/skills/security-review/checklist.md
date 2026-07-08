# 보안 점검 체크리스트 (YouthPick 백엔드)

## 인증 / 세션 / OAuth

- [ ] 미인증 접근이 리다이렉트가 아니라 `RestAuthenticationEntryPoint`에서 JSON + `A001 UNAUTHORIZED`로 내려가는가.
- [ ] 세션이 Spring Session(Redis)에 저장되는가. in-memory 세션에 의존하는 코드가 새로 생기지 않았는가.
- [ ] 인가 규칙(경로별 권한)이 `SecurityConfig`에 모여 있는가. Controller에 `if (권한)` 식으로 흩뿌려지지 않았는가.
- [ ] 로그인/회원가입에서 비밀번호가 평문 저장·로그되지 않는가. 인증 실패 메시지가 아이디 존재 여부를 흘리지 않는가(`A002`로 통일).
- [ ] OAuth 콜백에서 state/redirect_uri 검증이 있는가. 외부에서 받은 값을 검증 없이 신뢰하지 않는가.

## Secret / 민감정보

- [ ] OAuth client id/secret 등이 하드코딩되지 않고 환경변수로 주입되는가. 새 secret은 `.env.example`에 placeholder만 추가됐는가.
- [ ] secret / token / 세션 값이 로그(`log.info` 등)·응답 DTO·예외 메시지에 노출되지 않는가.
- [ ] `.env`가 커밋 대상에 포함되지 않았는가.
- [ ] 응답 DTO가 Entity의 민감 필드(비밀번호 해시, 내부 상태, raw OAuth payload)를 노출하지 않는가.

## 입력 검증 / 주입

- [ ] 모든 요청 DTO 필드에 Bean Validation이 붙어 있고 Controller가 `@Valid @RequestBody`로 받는가.
- [ ] JPA 쿼리에 사용자 입력을 문자열 연결로 넣지 않는가(파라미터 바인딩 사용).
- [ ] 한글/특수문자 파라미터 처리에서 인코딩 문제로 검증이 우회되지 않는가.

## 예외 / 정보 노출

- [ ] 예외 응답이 `GlobalExceptionHandler`를 거쳐 `ErrorResponse`로 통일되는가. stack trace/SQL/내부 클래스명이 응답에 노출되지 않는가.
- [ ] 예상 못 한 예외가 `S001`로 뭉뚱그려지고 상세가 클라이언트로 새지 않는가.

## 인가 / 소유권

- [ ] 즐겨찾기·자가진단 등 사용자 소유 리소스 조회/수정 시 "본인 것인지" 확인하는가(IDOR 방지).
- [ ] 관리자/일반 사용자 권한 경계가 명확한가.
