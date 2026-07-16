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
| 맞춤 추천 | 맞춤정책 조회 | `GET` | `/api/v1/recommended-policies` | 회원 | query: region 선택, category 선택, keyword 선택 |
| 최근 본 정책 | 최근 본 정책 목록 | `GET` | `/api/v1/policy-recent-views` | 회원 | query: page 기본 0, size 기본 20 |
| 관심 정책 | 관심정책 목록 | `GET` | `/api/v1/policy-applications` | 회원 | 없음 |
| 관심 정책 | 관심정책 등록 | `PUT` | `/api/v1/policy-applications/{policyId}` | 회원 | path: policyId |
| 관심 정책 | 관심정책 해제 | `DELETE` | `/api/v1/policy-applications/{policyId}` | 회원 | path: policyId |
| 읽음 상태 | 읽음 상태 목록 조회 | `GET` | `/api/v1/policy-read-states` | 회원 | query: policyIds 반복 |
| 읽음 상태 | 정책 읽음 처리 | `PUT` | `/api/v1/policy-read-states/{policyId}` | 회원 | path: policyId |
| 정책 비교 | 정책 비교 생성 | `POST` | `/api/v1/policy-comparisons` | 비회원 | body: policyIds[] |
| 정책 비교 | 정책 비교 조회 | `GET` | `/api/v1/policy-comparisons/{comparisonId}` | 비회원 | path: comparisonId |
| 정책 동기화 | 정책 수집 이력 목록 | `GET` | `/api/v1/policy-batch-histories` | 관리자 | query: page 기본 0, size 기본 20 |
| 정책 동기화 | 정책 수집 실행 | `POST` | `/api/v1/policy-batch-histories` | 관리자 | body: mode(FULL 또는 DELTA) |
| 정책 동기화 | 검색 인덱스 재생성 | `POST` | `/api/v1/search-indexes/rebuild` | 관리자 | 없음 |
| 관리자/운영 | 운영 지표 조회 | `GET` | `/api/v1/admin/metrics` | 관리자 | 없음 |
| 챗봇 | 챗봇 일반 질의 | `POST` | `/chat` | 비회원 | body: message, thread_id 선택, user_profile 선택 |
| 챗봇 | 챗봇 스트리밍 질의 | `POST` | `/chat/stream` | 비회원 | body: message, thread_id 선택, user_profile 선택 |

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

## 맞춤 추천

### 맞춤정책 조회

로그인 사용자의 프로필과 조건을 바탕으로 맞춤 정책을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/recommended-policies` |
| 권한 | 회원 |
| 파라미터 | query: region 선택, category 선택, keyword 선택 |

## 최근 본 정책

### 최근 본 정책 목록

로그인 사용자가 최근에 상세 조회한 정책 목록을 마지막 조회 시각 내림차순으로 조회한다. 같은 정책을 다시 보면 기록이 늘어나지 않고 조회 시각만 갱신된다. 삭제·숨김 처리된 정책은 목록에서 제외된다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-recent-views` |
| 권한 | 회원 |
| 파라미터 | query: page 기본 0, size 기본 20 |

## 관심 정책

### 관심정책 목록

로그인 사용자가 저장한 관심 정책 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-applications` |
| 권한 | 회원 |
| 파라미터 | 없음 |

### 관심정책 등록

특정 정책을 로그인 사용자의 관심 정책으로 저장한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `PUT` |
| 경로 | `/api/v1/policy-applications/{policyId}` |
| 권한 | 회원 |
| 파라미터 | path: policyId |

### 관심정책 해제

특정 정책을 로그인 사용자의 관심 정책 목록에서 제거한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `DELETE` |
| 경로 | `/api/v1/policy-applications/{policyId}` |
| 권한 | 회원 |
| 파라미터 | path: policyId |

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

### 정책 비교 생성

여러 정책 ID를 기준으로 비교 결과를 생성한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/api/v1/policy-comparisons` |
| 권한 | 비회원 |
| 파라미터 | body: policyIds[] |

### 정책 비교 조회

생성된 정책 비교 결과를 비교 ID로 조회한다.

| 항목 | 내용 |
|---|---|
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-comparisons/{comparisonId}` |
| 권한 | 비회원 |
| 파라미터 | path: comparisonId |

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

## 챗봇 FastAPI

아래 경로는 프론트가 FastAPI 챗봇 서비스로 직접 호출하는 API다. Spring Boot 백엔드 엔티티 기반 경로와 별도로 취급하며, Spring Controller로 중복 구현하지 않는다.

### 챗봇 일반 질의

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/chat` |
| 권한 | 비회원 |
| 파라미터 | body: message, thread_id 선택, user_profile 선택 |

### 챗봇 스트리밍 질의

| 항목 | 내용 |
|---|---|
| 메서드 | `POST` |
| 경로 | `/chat/stream` |
| 권한 | 비회원 |
| 파라미터 | body: message, thread_id 선택, user_profile 선택 |

## 기존/보류 명세

아래 항목은 Notion DB에 유지된 기존 데이터다. 현재 프론트 필요 API 경로 변경 작업의 직접 대상이 아니므로 별도 확인 후 사용한다.

| 기능 | 이름 | 메서드 | 경로 | 권한 | 파라미터 |
|---|---|---|---|---|---|
| 온보딩/프로필 | 프로필 삭제 | `DELETE` | `/api/v1/me/profile` | 회원 | 없음 |
| 정책 동기화 | 정책 수집 상세 | `GET` | `/api/v1/admin/policy-sync-jobs/{jobId}` | 관리자 | path: jobId |
| 관리자/운영 | 헬스 체크 | `GET` | `/api/v1/health` | 공통 | 없음 |
| 챗봇 | 챗봇 프로필 사용 동의 설정 | `POST` | `/api/v1/policy-chat/profile-consent` | 회원 | 명세 기준 확인 필요 |
| 챗봇 | 정책 탐색 챗봇 질문 | `POST` | `/api/v1/policy-chat/queries` | 비회원 | 명세 기준 확인 필요 |

## 백엔드 구현 메모

사용자별 데이터는 JWT에서 얻은 사용자 식별자를 서비스 계층에서 사용한다. URL 경로는 `user-profiles`, `policy-applications`, `policy-read-states`처럼 엔티티 리소스를 직접 드러내고, 컨트롤러 내부에서 인증 principal과 매핑한다.

`policy_applications`는 기존 즐겨찾기 개념을 흡수한 신청관리 엔티티다. 관심정책 등록과 해제는 별도 `favorites` 리소스보다 `policy-applications`의 생성/삭제 또는 상태 변경으로 구현하는 편이 현재 스키마와 맞다.

`policy-batch-histories`는 정책 수집 작업 이력을 나타낸다. 수집 실행은 같은 컬렉션에 작업 요청을 생성하는 `POST /api/v1/policy-batch-histories`로 해석한다.
