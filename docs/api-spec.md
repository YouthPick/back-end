# API 명세 (프론트 연동 기준)

`YouthPick/front-end` 코드와 Notion `API 명세 DB`를 기준으로 정리한 API 계약이다. 백엔드 API를 추가하거나 수정할 때 이 문서를 기준으로 컨트롤러 경로, HTTP 메서드, 파라미터, 권한을 맞춘다.

Spring Boot 백엔드는 `/api/v1/**` 엔드포인트를 담당한다. 챗봇은 FastAPI 서비스가 `/chat`, `/chat/stream`을 직접 제공하며, Spring Boot 컨트롤러로 중복 구현하지 않는다.

## 구현 전제

- Spring Boot API는 공통 응답 래퍼 `ApiResponse<T>`(data + meta)를 사용한다. 자세한 응답/에러 포맷은 [`../.claude/rules/api-design.md`](../.claude/rules/api-design.md), [`../.claude/rules/error-handling.md`](../.claude/rules/error-handling.md)를 따른다.
- 프론트 공통 API 클라이언트는 `VITE_API_BASE_URL` 뒤에 명세의 경로를 붙여 호출하며, 인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더를 전달한다.
- 401 응답이 발생하면 프론트는 `/api/v1/auth/token/refresh`로 토큰을 갱신한 뒤 원 요청을 한 번 재시도한다. 단 refresh API 자체는 공통 `requestJson`이 아니라 직접 `fetch`로 호출하므로, 401 재시도 루프 없이 성공 시 새 토큰 쌍을 반환하고 실패 시 클라이언트가 로컬 인증 상태를 지우게 만든다.
- FastAPI 챗봇 API는 Spring Boot API 경로가 아니라 `VITE_CHATBOT_API_BASE_URL` 뒤에 `/chat` 또는 `/chat/stream`을 붙여 호출한다.

## 기능 분류 기준

| 기능 분류 | 포함 범위 |
|---|---|
| 인증 | 로그인 사용자 조회, OAuth 인가 URL, OAuth 콜백, 토큰 갱신, 로그아웃, 회원 탈퇴 |
| 온보딩/프로필 | 프로필 선택지 조회, 내 프로필 조회, 프로필 저장/수정 |
| 정책 검색 | 정책 목록 검색, 정책 상세, 검색어 제안 |
| 맞춤 추천 | 로그인 사용자 맞춤 정책 추천 |
| 관심 정책 | 관심정책 목록, 등록, 해제 |
| 읽음 상태 | 정책 읽음 상태 조회와 읽음 처리 |
| 정책 비교 | 정책 비교 생성과 비교 결과 조회 |
| 정책 동기화 | 정책 수집 작업 목록, 실행, 검색 인덱스 재생성 |
| 관리자/운영 | 관리자 운영 지표와 운영성 엔드포인트 |
| 챗봇 | FastAPI 챗봇 질의, 스트리밍 질의, 기존 챗봇 관련 명세 |

## 공통 규칙

| 항목 | 내용 |
|---|---|
| Spring Boot base path | `/api/v1` |
| Spring Boot response wrapper | `ApiResponse<T>` 형태, 주요 데이터는 `data` 필드에 담는다. |
| 인증 헤더 | `Authorization: Bearer {accessToken}` |
| JSON 요청 | `Content-Type: application/json` |
| OAuth provider | `kakao`, `naver`, `google` |
| 읽음 상태 | `UNREAD`, `READ`, `NEEDS_RECHECK` |
| 추천 confidence | `HIGH`, `MEDIUM`, `LOW` |
| 동기화 mode | `FULL`, `DELTA` |
| 작업 status | `REQUESTED`, `RUNNING`, `SUCCEEDED`, `FAILED` |

## 현재 프론트 연동 API 요약

| 기능 분류 | 이름 | 메서드 | 경로 | 권한 | 파라미터 |
|---|---|---|---|---|---|
| 인증 | 로그아웃 | `POST` | `/api/v1/auth/logout` | 회원 | body: refreshToken |
| 인증 | 내 로그인 사용자 조회 | `GET` | `/api/v1/auth/me` | 회원 | 없음 |
| 인증 | 회원 탈퇴 | `DELETE` | `/api/v1/auth/me` | 회원 | body: confirmText="탈퇴합니다" |
| 인증 | OAuth 인가 URL 생성 | `GET` | `/api/v1/auth/oauth/{provider}/authorization-url` | 비회원 | path: provider(kakao, naver, google). query: redirectUri 필수, state 선택 |
| 인증 | 소셜 로그인 콜백 | `POST` | `/api/v1/auth/oauth/{provider}/callback` | 비회원 | path: provider(kakao, naver, google). body: code, redirectUri |
| 인증 | 액세스 토큰 갱신 | `POST` | `/api/v1/auth/token/refresh` | 비회원 | body: refreshToken |
| 온보딩/프로필 | 내 프로필 조회 | `GET` | `/api/v1/me/profile` | 회원 | 없음 |
| 온보딩/프로필 | 프로필 저장/수정 | `PUT` | `/api/v1/me/profile` | 회원 | body: birthYear, region, subRegion, employmentStatus, educationLevel, categories[], keywords[] |
| 온보딩/프로필 | 프로필 선택지 조회 | `GET` | `/api/v1/meta/profile-options` | 비회원 | 없음 |
| 정책 검색 | 정책 검색/필터 | `GET` | `/api/v1/policies` | 비회원 | query: keyword 선택, region 선택, category 선택, page 기본 0, size 기본 20, sort 기본 relevance |
| 정책 검색 | 검색어 제안 | `GET` | `/api/v1/policies/search-suggestions` | 비회원 | query: keyword 선택 |
| 정책 검색 | 정책 상세 조회 | `GET` | `/api/v1/policies/{policyId}` | 비회원 | path: policyId |
| 맞춤 추천 | 맞춤정책 조회 | `GET` | `/api/v1/me/recommended-policies` | 회원 | query: region 선택, category 선택, keyword 선택 |
| 관심 정책 | 관심정책 목록 | `GET` | `/api/v1/me/favorites` | 회원 | 없음 |
| 관심 정책 | 관심정책 등록 | `PUT` | `/api/v1/me/favorites/{policyId}` | 회원 | path: policyId |
| 관심 정책 | 관심정책 해제 | `DELETE` | `/api/v1/me/favorites/{policyId}` | 회원 | path: policyId |
| 읽음 상태 | 읽음 상태 목록 조회 | `GET` | `/api/v1/me/policy-read-states` | 회원 | query: policyIds 반복 |
| 읽음 상태 | 정책 읽음 처리 | `PUT` | `/api/v1/me/policy-read-states/{policyId}` | 회원 | path: policyId |
| 정책 비교 | 정책 비교 생성 | `POST` | `/api/v1/policy-comparisons` | 비회원 | body: policyIds[] |
| 정책 비교 | 정책 비교 조회 | `GET` | `/api/v1/policy-comparisons/{comparisonId}` | 비회원 | path: comparisonId |
| 정책 동기화 | 정책 수집 이력 목록 | `GET` | `/api/v1/admin/policy-sync-jobs` | 관리자 | query: page 기본 0, size 기본 20 |
| 정책 동기화 | 정책 수집 실행 | `POST` | `/api/v1/admin/policy-sync-jobs` | 관리자 | body: mode(FULL 또는 DELTA) |
| 정책 동기화 | 검색 인덱스 재생성 | `POST` | `/api/v1/admin/search-indexes/rebuild` | 관리자 | 없음 |
| 관리자/운영 | 운영 지표 조회 | `GET` | `/api/v1/admin/metrics` | 관리자 | 없음 |
| 관리자/운영 | 헬스 체크 | `GET` | `/api/v1/health` | 공통 | 없음 |
| 챗봇 | 챗봇 일반 질의 | `POST` | `/chat` | 비회원 | body: message, thread_id 선택, user_profile 선택 |
| 챗봇 | 챗봇 스트리밍 질의 | `POST` | `/chat/stream` | 비회원 | body: message, thread_id 선택, user_profile 선택 |

## 인증 API

인증 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 로그아웃

현재 로그인 세션을 종료하고 refresh token을 무효화한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/logout` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | body: refreshToken |

### 내 로그인 사용자 조회

현재 액세스 토큰 기준 로그인 사용자와 권한 정보를 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `GET` |
| 경로 | `/api/v1/auth/me` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | 없음 |

### 회원 탈퇴

현재 로그인 사용자의 계정을 탈퇴 처리한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `DELETE` |
| 경로 | `/api/v1/auth/me` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | body: confirmText="탈퇴합니다" |

### OAuth 인가 URL 생성

소셜 로그인 시작을 위한 OAuth 제공자별 인가 URL을 생성한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `GET` |
| 경로 | `/api/v1/auth/oauth/{provider}/authorization-url` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | path: provider(kakao, naver, google). query: redirectUri 필수, state 선택 |

### 소셜 로그인 콜백

OAuth 인가 코드로 로그인을 완료하고 사용자 정보와 토큰을 발급받는다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/oauth/{provider}/callback` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | path: provider(kakao, naver, google). body: code, redirectUri |

### 액세스 토큰 갱신

저장된 refresh token으로 새 access token과 refresh token을 발급받는다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 인증 |
| 메서드 | `POST` |
| 경로 | `/api/v1/auth/token/refresh` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | body: refreshToken |

## 온보딩/프로필 API

온보딩/프로필 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 내 프로필 조회

로그인 사용자의 저장된 정책 추천 프로필을 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 온보딩/프로필 |
| 메서드 | `GET` |
| 경로 | `/api/v1/me/profile` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | 없음 |

### 프로필 저장/수정

로그인 사용자의 정책 추천 프로필을 저장하거나 수정한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 온보딩/프로필 |
| 메서드 | `PUT` |
| 경로 | `/api/v1/me/profile` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | body: birthYear, region, subRegion, employmentStatus, educationLevel, categories[], keywords[] |

### 프로필 선택지 조회

온보딩 프로필 입력에 필요한 지역, 상태, 카테고리, 키워드 선택지를 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 온보딩/프로필 |
| 메서드 | `GET` |
| 경로 | `/api/v1/meta/profile-options` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | 없음 |

## 정책 검색 API

정책 검색 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 정책 검색/필터

정책 목록을 키워드와 필터 조건으로 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 검색 |
| 메서드 | `GET` |
| 경로 | `/api/v1/policies` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | query: keyword 선택, region 선택, category 선택, page 기본 0, size 기본 20, sort 기본 relevance |

### 검색어 제안

입력 중인 키워드 기준으로 정책 검색어 추천 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 검색 |
| 메서드 | `GET` |
| 경로 | `/api/v1/policies/search-suggestions` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | query: keyword 선택 |

### 정책 상세 조회

단일 정책의 상세 정보를 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 검색 |
| 메서드 | `GET` |
| 경로 | `/api/v1/policies/{policyId}` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | path: policyId |

## 맞춤 추천 API

맞춤 추천 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 맞춤정책 조회

로그인 사용자의 프로필과 조건을 바탕으로 맞춤 정책을 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 맞춤 추천 |
| 메서드 | `GET` |
| 경로 | `/api/v1/me/recommended-policies` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | query: region 선택, category 선택, keyword 선택 |

## 관심 정책 API

관심 정책 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 관심정책 목록

로그인 사용자가 저장한 관심 정책 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 관심 정책 |
| 메서드 | `GET` |
| 경로 | `/api/v1/me/favorites` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | 없음 |

### 관심정책 등록

특정 정책을 로그인 사용자의 관심 정책으로 저장한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 관심 정책 |
| 메서드 | `PUT` |
| 경로 | `/api/v1/me/favorites/{policyId}` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | path: policyId |

### 관심정책 해제

특정 정책을 로그인 사용자의 관심 정책 목록에서 제거한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 관심 정책 |
| 메서드 | `DELETE` |
| 경로 | `/api/v1/me/favorites/{policyId}` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | path: policyId |

## 읽음 상태 API

읽음 상태 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 읽음 상태 목록 조회

여러 정책에 대한 로그인 사용자의 읽음 상태를 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 읽음 상태 |
| 메서드 | `GET` |
| 경로 | `/api/v1/me/policy-read-states` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | query: policyIds 반복 |

### 정책 읽음 처리

특정 정책을 읽음 상태로 표시한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 읽음 상태 |
| 메서드 | `PUT` |
| 경로 | `/api/v1/me/policy-read-states/{policyId}` |
| 권한 | 회원 |
| 상태 | 확정 |
| 파라미터 | path: policyId |

## 정책 비교 API

정책 비교 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 정책 비교 생성

여러 정책 ID를 기준으로 비교 결과를 생성한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 비교 |
| 메서드 | `POST` |
| 경로 | `/api/v1/policy-comparisons` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | body: policyIds[] |

### 정책 비교 조회

생성된 정책 비교 결과를 비교 ID로 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 비교 |
| 메서드 | `GET` |
| 경로 | `/api/v1/policy-comparisons/{comparisonId}` |
| 권한 | 비회원 |
| 상태 | 확정 |
| 파라미터 | path: comparisonId |

## 정책 동기화 API

정책 동기화 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 정책 수집 이력 목록

관리자용 정책 동기화 작업 이력 목록을 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 동기화 |
| 메서드 | `GET` |
| 경로 | `/api/v1/admin/policy-sync-jobs` |
| 권한 | 관리자 |
| 상태 | 확정 |
| 파라미터 | query: page 기본 0, size 기본 20 |

### 정책 수집 실행

관리자가 정책 데이터 수집 작업을 시작한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 동기화 |
| 메서드 | `POST` |
| 경로 | `/api/v1/admin/policy-sync-jobs` |
| 권한 | 관리자 |
| 상태 | 확정 |
| 파라미터 | body: mode(FULL 또는 DELTA) |

### 검색 인덱스 재생성

관리자가 정책 검색 인덱스 재생성 작업을 요청한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 정책 동기화 |
| 메서드 | `POST` |
| 경로 | `/api/v1/admin/search-indexes/rebuild` |
| 권한 | 관리자 |
| 상태 | 확정 |
| 파라미터 | 없음 |

## 관리자/운영 API

관리자/운영 API는 백엔드에서 같은 도메인 책임으로 묶어 컨트롤러, 서비스, DTO를 설계할 수 있는 엔드포인트다. 각 항목은 프론트 코드가 기대하는 최소 계약을 기준으로 작성했다.

### 운영 지표 조회

관리자 화면에서 사용할 정책, 사용자, 동기화 운영 지표를 조회한다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 관리자/운영 |
| 메서드 | `GET` |
| 경로 | `/api/v1/admin/metrics` |
| 권한 | 관리자 |
| 상태 | 확정 |
| 파라미터 | 없음 |

### 헬스 체크

서비스 상태 확인용 헬스 체크 API다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 관리자/운영 |
| 메서드 | `GET` |
| 경로 | `/api/v1/health` |
| 권한 | 공통 |
| 상태 | 확정 |
| 파라미터 | 없음 |

## FastAPI 챗봇 API

챗봇 API는 한글 기능 분류상 `챗봇`에 속하지만, 현재 프론트가 FastAPI 챗봇 서비스에 직접 호출하는 경로다. Spring Boot 백엔드 에이전트는 이 경로를 구현 대상으로 보지 말고, 인프라 프록시나 CORS 설정을 맞출 때만 참고한다.

### 챗봇 일반 질의

FastAPI 챗봇에 일반 정책 질의를 보내고 최종 답변을 받는다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 챗봇 |
| 메서드 | `POST` |
| 경로 | `/chat` |
| 권한 | 비회원 |
| 파라미터 | body: message, thread_id 선택, user_profile 선택 |

### 챗봇 스트리밍 질의

FastAPI 챗봇에 정책 질의를 보내고 SSE 스트림으로 진행 상태와 최종 답변을 받는다.

| 항목 | 내용 |
|---|---|
| 기능 분류 | 챗봇 |
| 메서드 | `POST` |
| 경로 | `/chat/stream` |
| 권한 | 비회원 |
| 파라미터 | body: message, thread_id 선택, user_profile 선택 |

## 현재 프론트 코드에서 직접 호출되지 않은 기존 명세

아래 항목은 Notion DB에 남아 있지만 현재 확인한 `YouthPick/front-end` 브랜치에서 직접 호출되지 않았다. 구현하거나 유지보수하기 전에 제품 요구사항과 프론트 연동 방향을 확인한다.

| 기능 분류 | 이름 | 메서드 | 경로 | 권한 | 설명 | 파라미터 |
|---|---|---|---|---|---|---|
| 온보딩/프로필 | 프로필 삭제 | `DELETE` | `/api/v1/me/profile` | 회원 | 로그인 사용자의 프로필 삭제 API 명세다. 현재 프론트 코드 호출은 확인되지 않았다. | 없음 |
| 정책 동기화 | 정책 수집 상세 | `GET` | `/api/v1/admin/policy-sync-jobs/{jobId}` | 관리자 | 관리자용 정책 동기화 작업 단건 상세를 조회하는 기존 명세다. 현재 프론트 코드 호출은 확인되지 않았다. | path: jobId |
| 챗봇 | 챗봇 프로필 사용 동의 설정 | `POST` | `/api/v1/policy-chat/profile-consent` | 회원 | 챗봇에서 사용자 프로필 사용 동의를 설정하는 기존 명세다. 현재 프론트 코드 호출은 확인되지 않았다. | 명세 기준 확인 필요 |
| 챗봇 | 정책 탐색 챗봇 질문 | `POST` | `/api/v1/policy-chat/queries` | 비회원 | 기존 Spring Boot 경유 정책 챗봇 질문 API 명세다. 현재 프론트 코드는 FastAPI /chat 또는 /chat/stream을 직접 호출한다. | 명세 기준 확인 필요 |

## 백엔드 에이전트 작업 체크리스트

API를 추가하거나 수정할 때는 먼저 한글 기능 분류와 권한을 확인한다. 컨트롤러 경로와 HTTP 메서드는 프론트 호출 코드와 정확히 맞춰야 하며, `파라미터` 항목에는 path 변수, query string, JSON body를 포함한다. 인증 헤더는 `권한` 항목을 기준으로 별도 처리한다.

인증 API를 구현할 때는 refresh 토큰 갱신 흐름을 별도로 다룬다. 프론트는 refresh API를 공통 `requestJson`이 아니라 직접 `fetch`로 호출하므로, 401 재시도 루프 없이 성공 시 새 토큰 쌍을 반환하고 실패 시 클라이언트가 로컬 인증 상태를 지우게 만든다.

관리자/운영 API와 정책 동기화 API는 역할을 분리해서 구현한다. 관리자/운영은 대시보드 지표 중심이고, 정책 동기화는 수집 작업 실행과 이력, 검색 인덱스 재생성처럼 작업성 엔드포인트를 담당한다.

정책 검색과 맞춤 추천 API는 장애 상황을 UI가 처리할 수 있게 설계한다. 정책 목록 응답에는 `degraded` 플래그가 포함되어 있으므로 검색 인덱스 일부 장애나 외부 데이터 지연이 있을 때 빈 실패보다 부분 응답과 상태 표시를 우선한다.
