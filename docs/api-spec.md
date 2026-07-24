# YouthPick 백엔드 API 경로 명세

Notion `API 명세 DB`의 현재 데이터를 기준으로 생성한 백엔드 API 경로 명세다. 경로는 `YouthPick/back-end`의 엔티티 설계 방향을 반영해 리소스 중심으로 정리했다. 사용자 식별은 JWT 인증 정보에서 얻는 전제이므로 사용자별 API 경로에 `/me` 또는 `/users/{userId}`를 붙이지 않는다.

백엔드 API를 추가하거나 수정할 때는 이 문서를 기준으로 컨트롤러 경로, HTTP 메서드, 파라미터, 권한을 맞춘다. 공통 응답 래퍼(`ApiResponse<T>`), Bean Validation, 에러코드 체계 같은 구현 규칙은 [`../.claude/rules/api-design.md`](../.claude/rules/api-design.md), [`../.claude/rules/error-handling.md`](../.claude/rules/error-handling.md)를 따르며 이 문서에서 중복 서술하지 않는다.

## 경로 설계 기준

| 기준 | 내용 |
|---|---|
| base path | `/api/v1` |
| 사용자 식별 | JWT 인증 정보에서 식별 |
| 사용자별 엔티티 경로 | `/me`, `/users/{userId}` 없이 엔티티 리소스명 사용 |
| 프로필 엔티티 | `/api/v1/user-profiles` |
| 정책 신청관리 엔티티 | `/api/v1/policy-applications` |
| 정책 신청관리 체크리스트 엔티티 | `/api/v1/policy-application-checklists` |
| 정책 배치 이력 엔티티 | `/api/v1/policy-batch-histories` |
| 챗봇 API | FastAPI 직접 호출 경로이므로 Spring Boot 구현 대상과 분리 |

## API 요약

| 기능 | 이름 | 메서드 | 경로 | 권한 | 파라미터 |
|---|---|---|---|---|---|
| 인증 | 로그아웃 | `POST` | `/api/v1/auth/logout` | 회원 | body: refreshToken |
| 인증 | 내 로그인 사용자 조회 | `GET` | `/api/v1/auth/me` | 회원 | 없음 |
| 인증 | OAuth 인가 URL 생성 | `GET` | `/api/v1/auth/oauth/{provider}/authorization-url` | 비회원 | path: provider(kakao, naver, google). 파라미터 없음(state는 서버가 생성해 Redis에 저장, redirect_uri는 서버 설정값 고정) |
| 인증 | 소셜 로그인 콜백 | `POST` | `/api/v1/auth/oauth/{provider}/callback` | 비회원 | path: provider(kakao, naver, google). body: code, state(서버가 발급한 값을 그대로 반환, CSRF 방어용) |
| 인증 | 액세스 토큰 갱신 | `POST` | `/api/v1/auth/token/refresh` | 비회원 | body: refreshToken |
| 인증 | 회원 탈퇴 | `DELETE` | `/api/v1/users` | 회원 | body: confirmText="탈퇴합니다" |
| 온보딩/프로필 | 프로필 선택지 조회 | `GET` | `/api/v1/profile-options` | 비회원 | 없음 |
| 온보딩/프로필 | 내 프로필 조회 | `GET` | `/api/v1/user-profiles` | 회원 | 없음 |
| 온보딩/프로필 | 프로필 저장/수정 | `PUT` | `/api/v1/user-profiles` | 회원 | body: birthYear, region, subRegion, employmentStatus, educationLevel, categories[], keywords[] |
| 정책 검색 | 정책 검색/필터 | `GET` | `/api/v1/policies` | 비회원 | query: keyword 선택, region 선택, category 선택, page 기본 0, size 기본 20, sort 기본 relevance |
| 정책 검색 | 검색어 제안 | `GET` | `/api/v1/policies/search-suggestions` | 비회원 | query: keyword 선택 |
| 정책 검색 | 정책 상세 조회 | `GET` | `/api/v1/policies/{policyId}` | 비회원 | path: policyId |
| 정책 채팅 | 메시지 이력 조회 | `GET` | `/api/v1/policies/{policyId}/chat/messages` | 회원 | path: policyId, query: afterId 기본 0 |
| 정책 채팅 | STOMP 연결 | `WebSocket` | `/api/ws` | 회원 | STOMP `CONNECT` header: `Authorization: Bearer <accessToken>` |
| 정책 채팅 | 실시간 메시지 전송 | `STOMP SEND` | `/app/policies/{policyId}/chat/messages` | 회원 | body: content(1~1000자) |
| 정책 채팅 | 실시간 메시지 수신 | `STOMP SUBSCRIBE` | `/user/queue/policies/{policyId}/chat/messages` | 회원 | path: policyId |
| 정책 채팅 | 오류 수신 | `STOMP SUBSCRIBE` | `/user/queue/policies/{policyId}/chat/errors` | 회원 | path: policyId |
| 맞춤 추천 | 맞춤정책 조회 | `GET` | `/api/v1/recommended-policies` | 회원 | query: region 선택, category 선택, keyword 선택 |
| 신청관리 | 신청 등록 | `POST` | `/api/v1/policy-applications` | 회원 | body: policyId, status(INTERESTED\|PREPARING\|APPLIED\|COMPLETED), memo 선택, endAt 선택 |
| 신청관리 | 신청 목록 조회 | `GET` | `/api/v1/policy-applications` | 회원 | query: page 기본 1, size 기본 20 |
| 신청관리 | 상태 변경 | `PATCH` | `/api/v1/policy-applications/{id}/status` | 회원 | path: id. query: status(INTERESTED\|PREPARING\|APPLIED\|COMPLETED) |
| 신청관리 | 메모 수정 | `PATCH` | `/api/v1/policy-applications/{id}/memo` | 회원 | path: id. body: memo (필수, 최대 2000자) |
| 신청관리 | 마감일 수정 | `PATCH` | `/api/v1/policy-applications/{id}/end-at` | 회원 | path: id. body: endAt 선택(ISO-8601 date-time, 생략 시 마감일 초기화) |
| 신청관리 | 신청 삭제 | `DELETE` | `/api/v1/policy-applications/{id}` | 회원 | path: id |
| 신청관리 | 체크리스트 추가 | `POST` | `/api/v1/policy-application-checklists` | 회원 | body: applicationId, message |
| 신청관리 | 체크리스트 조회 | `GET` | `/api/v1/policy-application-checklists/application/{applicationId}` | 회원 | path: applicationId. query: page 기본 1, size 기본 20 |
| 신청관리 | 체크리스트 체크 | `PATCH` | `/api/v1/policy-application-checklists/{id}/check` | 회원 | path: id |
| 신청관리 | 체크리스트 체크 해제 | `PATCH` | `/api/v1/policy-application-checklists/{id}/uncheck` | 회원 | path: id |
| 신청관리 | 체크리스트 삭제 | `DELETE` | `/api/v1/policy-application-checklists/{id}` | 회원 | path: id |
| 최근 본 정책 | 최근 본 정책 목록 | `GET` | `/api/v1/policy-recent-views` | 회원 | query: page 기본 0, size 기본 20 |
| 읽음 상태 | 읽음 상태 목록 조회 | `GET` | `/api/v1/policy-read-states` | 회원 | query: policyIds 반복 |
| 읽음 상태 | 정책 읽음 처리 | `PUT` | `/api/v1/policy-read-states/{policyId}` | 회원 | path: policyId |
| 정책 비교 | 정책 비교 조회 | `GET` | `/api/v1/policy-comparisons` | 비회원 | query: policyIds 반복(2~3개) |
| 정책 동기화 | 정책 수집 이력 목록 | `GET` | `/api/v1/policy-batch-histories` | 관리자 | query: page 기본 0, size 기본 20 |
| 정책 동기화 | 정책 수집 실행 | `POST` | `/api/v1/policy-batch-histories` | 관리자 | body: mode(FULL 또는 DELTA) |
| 정책 동기화 | 검색 인덱스 재생성 | `POST` | `/api/v1/search-indexes/rebuild` | 관리자 | 없음 |
| 관리자/운영 | 운영 지표 조회 | `GET` | `/api/v1/admin/metrics` | 관리자 | 없음 |

## 인증

### 로그아웃

현재 로그인 세션을 종료하고 refresh token을 무효화한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/logout` |
| 권한 | 회원 |
| 파라미터 | body: refreshToken |

### 내 로그인 사용자 조회

현재 액세스 토큰 기준 로그인 사용자와 권한 정보를 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/auth/me` |
| 권한 | 회원 |
| 파라미터 | 없음 |

### OAuth 인가 URL 생성

소셜 로그인 시작을 위한 OAuth 제공자별 인가 URL을 생성한다. CSRF 방어용 `state`는 서버가 생성해 Redis에 짧은 TTL로 저장하고 응답 URL에 포함시킨다. provider로 리다이렉트할 `redirect_uri`도 서버 설정값(`youthpick.oauth.frontend-callback-uri`)으로 고정되어 있어 프론트가 별도로 넘길 파라미터는 없다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/auth/oauth/{provider}/authorization-url` |
| 권한 | 비회원 |
| 파라미터 | path: provider(kakao, naver, google). 그 외 파라미터 없음 |

### 소셜 로그인 콜백

OAuth 인가 코드로 로그인을 완료하고 사용자 정보와 토큰을 발급받는다. 프론트는 자신의 콜백 라우트(`redirect_uri`)에서 provider가 붙여준 `code`/`state`를 그대로 읽어 이 API로 전달한다. `state`는 authorization-url 발급 시 서버가 준 값과 일치해야 하며, 1회 사용 후 폐기된다(CSRF 방어).

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/oauth/{provider}/callback` |
| 권한 | 비회원 |
| 파라미터 | path: provider(kakao, naver, google). body: code, state |

### 액세스 토큰 갱신

저장된 refresh token으로 새 access token과 refresh token을 발급받는다. 프론트는 이 API를 공통 `requestJson`이 아니라 직접 `fetch`로 호출하므로, 401 재시도 루프 없이 성공 시 새 토큰 쌍을 반환하고 실패 시 클라이언트가 로컬 인증 상태를 지우게 만든다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/token/refresh` |
| 권한 | 비회원 |
| 파라미터 | body: refreshToken |

### 회원 탈퇴

현재 로그인 사용자의 계정을 탈퇴 처리한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `DELETE` |
| 경로 | `/api/v1/users` |
| 권한 | 회원 |
| 파라미터 | body: confirmText="탈퇴합니다" |

## 온보딩/프로필

### 프로필 선택지 조회

온보딩 프로필 입력에 필요한 지역, 상태, 카테고리, 키워드 선택지를 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/profile-options` |
| 권한 | 비회원 |
| 파라미터 | 없음 |

### 내 프로필 조회

로그인 사용자의 저장된 정책 추천 프로필을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/user-profiles` |
| 권한 | 회원 |
| 파라미터 | 없음 |

### 프로필 저장/수정

로그인 사용자의 정책 추천 프로필을 저장하거나 수정한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PUT` |
| 경로 | `/api/v1/user-profiles` |
| 권한 | 회원 |
| 파라미터 | body: birthYear, region, subRegion, employmentStatus, educationLevel, categories[], keywords[] |

## 정책 검색

### 정책 검색/필터

정책 목록을 키워드와 필터 조건으로 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policies` |
| 권한 | 비회원 |
| 파라미터 | query: keyword 선택, region 선택, category 선택, page 기본 0, size 기본 20, sort 기본 relevance |

### 검색어 제안

입력 중인 키워드 기준으로 정책 검색어 추천 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policies/search-suggestions` |
| 권한 | 비회원 |
| 파라미터 | query: keyword 선택 |

### 정책 상세 조회

단일 정책의 상세 정보를 조회한다. 삭제·숨김 처리된 정책은 404(`P001`)로 응답한다. 로그인 사용자(access token 포함 요청)가 조회하면 해당 정책이 최근 본 정책으로 기록된다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policies/{policyId}` |
| 권한 | 비회원 |
| 파라미터 | path: policyId |

## 정책 채팅

삭제되지 않은 `VISIBLE` 정책에서 로그인 회원끼리 메시지를 주고받는다. 숨김·삭제된 정책은 이력 조회, 전송, 구독 모두 거부한다. 메시지는 정책별 ID 오름차순이며 `afterId`를 제외한 이후 메시지만 반환한다.

### 메시지 이력 조회

현재 저장된 메시지를 즉시 반환한다. 실시간 수신은 아래 STOMP 구독을 사용한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policies/{policyId}/chat/messages` |
| 권한 | 회원 |
| 파라미터 | path: policyId. query: afterId 기본 0 |
| 응답 data | `messages[{id, policyId, authorName, content, createdAt, mine}]`, `nextCursor` |

### STOMP 연결과 목적지

브라우저 WebSocket handshake에는 토큰을 넣지 않는다. 클라이언트는 `/api/ws`에 native WebSocket으로 연결한 뒤 STOMP `CONNECT` frame의 `Authorization` native header에 access token을 보낸다. 토큰이 없거나 유효하지 않으면 연결을 거부한다.

| 항목 | 내용 |
|---|---|
| WebSocket endpoint | `/api/ws` |
| application prefix | `/app` |
| user destination prefix | `/user` |
| broker prefix | `/queue`만 사용. 정책 채팅에 `/topic` broadcast를 사용하지 않음 |
| 전송 | `SEND /app/policies/{policyId}/chat/messages`, body: `{content}`(필수, 공백 불가, 최대 1000자) |
| 메시지 구독 | `SUBSCRIBE /user/queue/policies/{policyId}/chat/messages` |
| 오류 구독 | `SUBSCRIBE /user/queue/policies/{policyId}/chat/errors` |

메시지는 DB commit이 끝난 뒤 해당 정책을 구독 중인 사용자에게만 개별 전송한다. 메시지 payload는 `{id, policyId, authorName, content, createdAt, mine}`이며 `mine`은 수신 사용자별로 계산한다. 사용자 id, 이메일, 소셜 provider, token은 노출하지 않는다. 오류 payload는 `{code, message}`다. 허용 목록 밖의 전역 목적지와 `/topic` SEND/SUBSCRIBE는 거부한다.

## 맞춤 추천

### 맞춤정책 조회

로그인 사용자의 프로필과 조건을 바탕으로 맞춤 정책을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/recommended-policies` |
| 권한 | 회원 |
| 파라미터 | query: region 선택, category 선택, keyword 선택 |

## 신청관리

기존 즐겨찾기(관심 정책) 개념을 흡수한 정책 신청관리 엔티티다. 상태(`ApplicationStatus`: `INTERESTED`/`PREPARING`/`APPLIED`/`COMPLETED`)·메모·마감일과 하위 체크리스트를 함께 관리한다.

### 신청 등록

정책을 신청관리 목록에 등록한다. 이미 등록된(soft-delete 되지 않은) 항목이면 `P002` 충돌 에러를 반환하고, soft-delete된 항목이면 재활성화하면서 기존 체크리스트를 초기화한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/policy-applications` |
| 권한 | 회원 |
| 파라미터 | body: policyId, status(INTERESTED\|PREPARING\|APPLIED\|COMPLETED), memo 선택, endAt 선택 |

### 신청 목록 조회

로그인 사용자의 신청관리 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-applications` |
| 권한 | 회원 |
| 파라미터 | query: page 기본 1, size 기본 20 |

### 상태 변경

신청관리 항목의 상태를 변경한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/policy-applications/{id}/status` |
| 권한 | 회원 |
| 파라미터 | path: id. query: status(INTERESTED\|PREPARING\|APPLIED\|COMPLETED) |

### 메모 수정

신청관리 항목의 개인 메모를 수정한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/policy-applications/{id}/memo` |
| 권한 | 회원 |
| 파라미터 | path: id. body: memo (필수, 최대 2000자) |

### 마감일 수정

신청관리 항목의 마감일(endAt)을 수정한다. endAt을 생략하면 필수값 오류가 아니라 마감일 초기화(clear)로 처리한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/policy-applications/{id}/end-at` |
| 권한 | 회원 |
| 파라미터 | path: id. body: endAt 선택(ISO-8601 date-time, 생략 시 마감일 초기화) |

### 신청 삭제

신청관리 항목을 삭제(soft-delete)한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `DELETE` |
| 경로 | `/api/v1/policy-applications/{id}` |
| 권한 | 회원 |
| 파라미터 | path: id |

### 체크리스트 추가

신청관리 항목에 체크리스트 항목을 추가한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/policy-application-checklists` |
| 권한 | 회원 |
| 파라미터 | body: applicationId, message (필수, 최대 500자) |

### 체크리스트 조회

특정 신청관리 항목의 체크리스트 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-application-checklists/application/{applicationId}` |
| 권한 | 회원 |
| 파라미터 | path: applicationId. query: page 기본 1, size 기본 20 |

### 체크리스트 체크

체크리스트 항목을 체크 완료 상태로 표시한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/policy-application-checklists/{id}/check` |
| 권한 | 회원 |
| 파라미터 | path: id |

### 체크리스트 체크 해제

체크리스트 항목의 체크를 해제한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PATCH` |
| 경로 | `/api/v1/policy-application-checklists/{id}/uncheck` |
| 권한 | 회원 |
| 파라미터 | path: id |

### 체크리스트 삭제

체크리스트 항목을 삭제(soft-delete)한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `DELETE` |
| 경로 | `/api/v1/policy-application-checklists/{id}` |
| 권한 | 회원 |
| 파라미터 | path: id |

## 최근 본 정책

### 최근 본 정책 목록

로그인 사용자가 최근에 상세 조회한 정책 목록을 마지막 조회 시각 내림차순으로 조회한다. 같은 정책을 다시 보면 기록이 늘어나지 않고 조회 시각만 갱신된다. 삭제·숨김 처리된 정책은 목록에서 제외된다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-recent-views` |
| 권한 | 회원 |
| 파라미터 | query: page 기본 0, size 기본 20 |

## 읽음 상태

### 읽음 상태 목록 조회

여러 정책에 대한 로그인 사용자의 읽음 상태를 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-read-states` |
| 권한 | 회원 |
| 파라미터 | query: policyIds 반복 |

### 정책 읽음 처리

특정 정책을 읽음 상태로 표시한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PUT` |
| 경로 | `/api/v1/policy-read-states/{policyId}` |
| 권한 | 회원 |
| 파라미터 | path: policyId |

## 정책 비교

비교 결과를 저장하지 않고 요청받은 정책들을 그때그때 조회해 내려주는 **순수 조회**다. 그래서 생성(`POST`) + 식별자(`comparisonId`) 재조회 구조가 아니라 쿼리 파라미터를 받는 **단일 `GET`**이다. 저장하는 것이 없으므로 `POST`는 만들 리소스가 없고, 식별자 역시 요청에 담긴 `policyIds`를 그대로 다시 표현한 값이라 정보를 더하지 않는다. 공유가 필요하면 이 요청 URL 자체가 공유 링크가 된다.

응답은 항상 조회 시점의 최신 정책 정보이며, 요청에 담긴 `policyIds` 순서를 그대로 보존한다(비교표 열 순서 = 사용자가 고른 순서).

### 정책 비교 조회

여러 정책을 나란히 비교할 수 있도록 항목별 값을 함께 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-comparisons` |
| 권한 | 비회원 |
| 파라미터 | query: `policyIds` 반복 — 서로 다른 정책 2~3개 (예: `?policyIds=1&policyIds=2`) |
| 에러 | 개수가 2~3개를 벗어나거나 숫자가 아니거나 누락되면 `C001`, 중복된 정책이 있으면 `P008 INVALID_COMPARISON_REQUEST`, 존재하지 않는 정책이 섞여 있으면 `P001 POLICY_NOT_FOUND` |

### 응답 필드

`data`는 비교 대상 정책 배열이다. 각 원소는 `Policy` 엔티티 전체가 아니라 비교에 필요한 필드만 담은 값이며, **정책 상세 조회 응답의 부분집합으로 유지한다** — 같은 정책을 상세로 볼 때 감춰지는 값이 비교로 볼 때만 드러나면 안 되기 때문이다. 비교표는 자격 원문(추가 자격조건·참여 제한사항 등)만 나란히 보여주면 되므로, 상세 응답이 노출하는 자격 판정용 내부 코드(`jobCodes`, `schoolCodes`, `majorCodes`, `specializationCodes`, `maritalStatusCode` — #92부터 정책 목록/상세 응답에 노출)와 보류 필드, raw payload는 비교 응답에는 포함하지 않는다.

| 필드 | 타입 | 내용 |
|---|---|---|
| `policyId` | number | 정책 ID |
| `title` | string | 정책명 |
| `category` | string | 대분류 |
| `organizationName` | string | 주관기관 |
| `minAge` / `maxAge` | number \| null | 지원 나이 범위 (null = 제한없음) |
| `incomeConditionCode` | string \| null | 소득 조건 구분 코드 |
| `incomeMaxAmount` | number \| null | 연소득 상한(만원) |
| `incomeEtcContent` | string \| null | 소득 조건 기타 설명 |
| `additionalQualification` | string \| null | 추가 자격조건 원문 |
| `participationRestriction` | string \| null | 참여 제한사항 원문 |
| `applicationEndDate` | string(`YYYY-MM-DD`) \| null | 신청 마감일 |
| `applicationUrl` | string \| null | 신청 바로가기 링크 |
| `regions[]` | array | 지원 지역 목록. 비어 있으면 지역 조건 없음 |
| `regions[].regionCode` | string | 법정동 코드 |
| `regions[].provinceName` | string | 시도명 |
| `regions[].districtName` | string | 시군구명 |

## 정책 동기화

### 정책 수집 이력 목록

관리자용 정책 동기화 작업 이력 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-batch-histories` |
| 권한 | 관리자 |
| 파라미터 | query: page 기본 0, size 기본 20 |

### 정책 수집 실행

관리자가 정책 데이터 수집 작업을 시작한다. 수집 실행은 같은 컬렉션에 작업 요청을 생성하는 것으로 해석한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/policy-batch-histories` |
| 권한 | 관리자 |
| 파라미터 | body: mode(FULL 또는 DELTA) |

### 검색 인덱스 재생성

관리자가 정책 검색 인덱스 재생성 작업을 요청한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/search-indexes/rebuild` |
| 권한 | 관리자 |
| 파라미터 | 없음 |

## 관리자/운영

### 운영 지표 조회

관리자 화면에서 사용할 정책, 사용자, 동기화 운영 지표를 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/admin/metrics` |
| 권한 | 관리자 |
| 파라미터 | 없음 |

## 기존/보류 명세

아래 항목은 Notion DB에 유지된 기존 데이터다. 현재 프론트 필요 API 경로 변경 작업의 직접 대상이 아니므로 별도 확인 후 사용한다.

| 기능 | 이름 | 메서드 | 경로 | 권한 | 파라미터 |
|---|---|---|---|---|---|
| 온보딩/프로필 | 프로필 삭제 | `DELETE` | `/api/v1/me/profile` | 회원 | 없음 |
| 정책 동기화 | 정책 수집 상세 | `GET` | `/api/v1/admin/policy-sync-jobs/{jobId}` | 관리자 | path: jobId |
| 관리자/운영 | 헬스 체크 | `GET` | `/api/v1/health` | 공통 | 없음 |

## 백엔드 구현 메모

사용자별 데이터는 JWT에서 얻은 사용자 식별자를 서비스 계층에서 사용한다. URL 경로는 `user-profiles`, `policy-applications`, `policy-read-states`처럼 엔티티 리소스를 직접 드러내고, 컨트롤러 내부에서 인증 principal과 매핑한다.

`policy_applications`는 기존 즐겨찾기 개념을 흡수한 신청관리 엔티티다. 관심정책 등록·해제는 별도 `favorites` 리소스가 아니라 `policy-applications`의 생성/삭제와 상태 변경(`status`)으로 구현되어 있다. 신청관리 하위 체크리스트는 `policy_application_checklists` 엔티티로 별도 관리하며, 신청관리 삭제·재등록(soft-delete → reactivate) 시 체크리스트도 함께 초기화된다.

`policy-batch-histories`는 정책 수집 작업 이력을 나타낸다. 수집 실행은 같은 컬렉션에 작업 요청을 생성하는 `POST /api/v1/policy-batch-histories`로 해석한다.
