# API 명세 DB

> 기획안의 기능을 기준으로 작성한 REST API 명세입니다.

| 이름 | 메서드 | 경로 | 권한 | 기능 | 상태 | 요청 | 응답 |
|---|---|---|---|---|---|---|---|
| 정책 수집 실행 | POST | `/api/v1/admin/policy-sync-jobs` | 관리자 | SYNC | 확정 |  |  |
| 로그아웃 | POST | `/api/v1/auth/logout` | 회원 | AUTH | 확정 |  |  |
| 정책 탐색 챗봇 질문 | POST | `/api/v1/policy-chat/queries` | 비회원 | CHAT | 확정 |  |  |
| 관심정책 등록 | PUT | `/api/v1/me/favorites/{policyId}` | 회원 | FAV | 확정 |  |  |
| 정책 비교 생성 | POST | `/api/v1/policy-comparisons` | 비회원 | CMP | 확정 |  |  |
| OAuth 인가 URL 생성 | GET | `/api/v1/auth/oauth/{provider}/authorization-url` | 비회원 | AUTH | 확정 |  |  |
| OAuth 콜백(로그인) | POST | `/api/v1/auth/oauth/{provider}/callback` | 비회원 | AUTH | 확정 | `{code, state}` | body: `{accessToken, tokenType, expiresIn}`, `Set-Cookie: refresh_token`(HttpOnly) |
| 액세스 토큰 재발급 | POST | `/api/v1/auth/token/refresh` | 비회원 | AUTH | 확정 | 쿠키: `refresh_token`(요청 body 없음) | body: `{accessToken, tokenType, expiresIn}`, `Set-Cookie: refresh_token`(HttpOnly, 재발급값으로 교체) |
| 프로필 선택지 조회 | GET | `/api/v1/meta/profile-options` | 비회원 | ONB | 확정 |  |  |
| 정책 수집 이력 목록 | GET | `/api/v1/admin/policy-sync-jobs` | 관리자 | SYNC | 확정 |  |  |
| 맞춤정책 조회 | GET | `/api/v1/me/recommended-policies` | 회원 | REC | 확정 |  |  |
| 회원 탈퇴 | DELETE | `/api/v1/auth/me` | 회원 | AUTH | 확정 |  |  |
| 정책 상세 조회 | GET | `/api/v1/policies/{policyId}` | 비회원 | SEARCH | 확정 |  |  |
| 챗봇 프로필 사용 동의 설정 | POST | `/api/v1/policy-chat/profile-consent` | 회원 | CHAT | 확정 |  |  |
| 읽음 상태 목록 조회 | GET | `/api/v1/me/policy-read-states` | 회원 | READ | 확정 |  |  |
| 관심정책 해제 | DELETE | `/api/v1/me/favorites/{policyId}` | 회원 | FAV | 확정 |  |  |
| 관심정책 목록 | GET | `/api/v1/me/favorites` | 회원 | FAV | 확정 |  |  |
| 프로필 저장/수정 | PUT | `/api/v1/me/profile` | 회원 | ONB | 확정 |  |  |
| 헬스 체크 | GET | `/api/v1/health` | 공통 | ADMIN | 확정 |  |  |
| 내 로그인 사용자 조회 | GET | `/api/v1/auth/me` | 회원 | AUTH | 확정 |  |  |
| 정책 비교 조회 | GET | `/api/v1/policy-comparisons/{comparisonId}` | 비회원 | CMP | 확정 |  |  |
| 정책 검색/필터 | GET | `/api/v1/policies` | 비회원 | SEARCH | 확정 |  |  |
