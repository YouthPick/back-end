# API 명세 DB

> 기획안의 기능을 기준으로 작성한 REST API 명세입니다.

| 이름 | 메서드 | 경로 | 권한 | 기능 | 상태 | 요청 | 응답 |
|---|---|---|---|---|---|---|---|
| 정책 수집 실행 | POST | `/api/v1/admin/policy-sync-jobs` | 관리자 | SYNC | 확정 |  |  |
| 로그아웃 | POST | `/api/v1/auth/logout` | 회원 | AUTH | 확정 |  |  |
| 관심정책 등록 | PUT | `/api/v1/me/favorites/{policyId}` | 회원 | FAV | 확정 |  |  |
| 정책 비교 생성 | POST | `/api/v1/policy-comparisons` | 비회원 | CMP | 확정 |  |  |
| OAuth 인가 URL 생성 | GET | `/api/v1/auth/oauth/{provider}/authorization-url` | 비회원 | AUTH | 확정 |  |  |
| OAuth 콜백(로그인) | POST | `/api/v1/auth/oauth/{provider}/callback` | 비회원 | AUTH | 확정 | `{code, state}` | body: `{accessToken, tokenType, expiresIn}`, `Set-Cookie: refresh_token`(HttpOnly) |
| 액세스 토큰 재발급 | POST | `/api/v1/auth/token/refresh` | 비회원 | AUTH | 확정 | 쿠키: `refresh_token`(요청 body 없음) | body: `{accessToken, tokenType, expiresIn}` (refresh token은 재발급하지 않으므로 `Set-Cookie` 없음) |
| 프로필 선택지 조회 | GET | `/api/v1/meta/profile-options` | 비회원 | ONB | 확정 |  |  |
| 정책 수집 이력 목록 | GET | `/api/v1/admin/policy-sync-jobs` | 관리자 | SYNC | 확정 |  |  |
| 정책 수집 요약 조회 | GET | `/api/v1/admin/policy-sync-jobs/summary` | 관리자 | SYNC | 확정 |  | body: `{activeCount, missingCount, parseErrorCount, dbFailCount}` — `parseErrorCount`/`dbFailCount`는 정책 수집 배치 파이프라인 미구현으로 항상 0 |
| 맞춤정책 조회 | GET | `/api/v1/me/recommended-policies` | 회원 | REC | 확정 |  |  |
| 회원 탈퇴 | DELETE | `/api/v1/auth/me` | 회원 | AUTH | 확정 |  |  |
| 정책 상세 조회 | GET | `/api/v1/policies/{policyId}` | 비회원 | SEARCH | 확정 |  |  |
| 읽음 상태 목록 조회 | GET | `/api/v1/me/policy-read-states` | 회원 | READ | 확정 |  |  |
| 관심정책 해제 | DELETE | `/api/v1/me/favorites/{policyId}` | 회원 | FAV | 확정 |  |  |
| 관심정책 목록 | GET | `/api/v1/me/favorites` | 회원 | FAV | 확정 |  |  |
| 프로필 저장/수정 | PUT | `/api/v1/me/profile` | 회원 | ONB | 확정 |  |  |
| 헬스 체크 | GET | `/api/v1/health` | 공통 | ADMIN | 확정 |  |  |
| 내 로그인 사용자 조회 | GET | `/api/v1/auth/me` | 회원 | AUTH | 확정 |  |  |
| 정책 비교 조회 | GET | `/api/v1/policy-comparisons/{comparisonId}` | 비회원 | CMP | 확정 |  |  |
| 정책 검색/필터 | GET | `/api/v1/policies` | 비회원 | SEARCH | 확정 |  |  |

## 정책 채팅 DB 스키마

`policy_chat_messages`는 정책별 회원 대화 메시지를 저장한다. 삭제되지 않은 정책 채팅 이력은 `(policy_id, id)` 인덱스로 커서 조회한다.

| 테이블 | 컬럼 | 제약 | 설명 |
|---|---|---|---|
| `policy_chat_messages` | `id` | PK, auto increment | 메시지 ID |
| `policy_chat_messages` | `policy_id` | NOT NULL, FK `policies(id)`, index `(policy_id, id)` | 메시지가 속한 정책 |
| `policy_chat_messages` | `user_id` | NOT NULL, FK `users(id)`, index | 작성 회원 |
| `policy_chat_messages` | `content` | NOT NULL, `VARCHAR(1000)` | 메시지 본문 |
| `policy_chat_messages` | `created_at` | NOT NULL, 기본 `CURRENT_TIMESTAMP` | 생성 시각 |
| `policy_chat_messages` | `updated_at` | NOT NULL, 기본 `CURRENT_TIMESTAMP`, 수정 시 자동 갱신 | 수정 시각 |
| `policy_chat_messages` | `deleted_at` | NULL | 소프트 삭제 시각 |
