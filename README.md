# YouthPick Back-end

청년을 위한 정책 추천 서비스 **YouthPick**의 백엔드 API 서버다. 정책 검색·추천, 신청 관리(트래커), 커뮤니티, 소셜 로그인/인증, 관리자 기능을 제공한다.

[![CI](https://github.com/YouthPick/back-end/actions/workflows/ci.yml/badge.svg)](https://github.com/YouthPick/back-end/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen)

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Language / Build | Java 21, Gradle Wrapper (Groovy DSL) |
| Framework | Spring Boot 3.5.16 (Web MVC, WebSocket) |
| 인증 | Spring Security (STATELESS) + JWT(access/refresh) + OAuth(Google/Naver/Kakao 자체 구현) |
| DB / ORM | MySQL 8 + Spring Data JPA + Flyway(`db/migration`, JPA는 `validate`만) |
| 테스트 DB | H2 in-memory (MySQL 모드) |
| Cache / Session | Redis (refresh token TTL 저장) |
| 파일 저장 | MinIO (S3 호환 오브젝트 스토리지) |
| 로깅 | Log4j2 (JDBC Appender로 애플리케이션 로그 DB 적재) |
| 코드 포맷 | Spotless (google-java-format, AOSP 스타일) |
| 테스트 | JUnit 5, Spring Security Test |

## 구현 특징

- **정책 상세 조회와 "최근 본 정책" 기록을 별도 트랜잭션으로 분리한다.** 조회 기록은 `REQUIRES_NEW`로 실행해, 기록 저장이 실패해도 그 실패가 상세 조회 응답 자체를 rollback-only로 끌고 내려가지 않는다.
- **배치 동기화는 Redis 락으로 중복 실행을 막는다.** 온통청년 공공 API 전량 수집 스케줄러는 SETNX+TTL+소유 토큰 방식의 락을 쓰고, 해제는 Lua 스크립트로 원자적으로 처리해 소유자가 아닌 프로세스가 잘못 해제하는 경우를 막는다.
- **외부 API 원본 데이터의 결측·형식 문제를 매퍼 계층에서 근본적으로 정규화한다.** 신청기간이 없는 정책의 폴백 처리, 참고 URL 형식 보정을 매퍼에 적용하고, 기존에 이미 적재된 데이터까지 백필하는 마이그레이션을 함께 실행했다.
- **refresh token은 rotate하지 않고 최초 TTL을 그대로 재사용한다.** HttpOnly 쿠키로만 전달하고(XSS 방어) 유효성의 근원은 Redis에 둔다. 재발급 시 access token만 새로 내려주고 쿠키는 다시 세팅하지 않는다.
- **정책 비교는 상태를 저장하지 않는 단일 `GET`이다.** 비교 대상 정책 ID를 쿼리 파라미터로 받아 그때그때 계산해 응답하므로, 요청 URL 자체가 그대로 공유 가능한 링크가 된다.
- **정책 신청(트래커)은 물리 삭제 대신 소프트 삭제 + 재활성화 구조다.** 관심 해제 후 다시 등록하면 새 행을 만들지 않고 기존 soft-deleted 행을 되살리며, 연결된 준비 체크리스트도 함께 정리한다.
- **정책 채팅(STOMP)은 프론트와 하트비트 주기를 명시적으로 맞춘다.** Spring `SimpleBroker`는 heartbeat 값을 선언하지 않으면 기본 비활성으로 협상되어 유휴 커넥션이 중간 인프라에서 조용히 끊길 수 있어, 전용 `TaskScheduler`로 10초 하트비트를 명시했다.
- **에러 응답은 도메인 접두어 체계를 가진 `ErrorCode`로 통일한다.** `C`(공통)·`A`(인증)·`U`(사용자)·`P`(정책) 등 접두어 + 3자리 코드로 프론트가 `code` 기준 메시지를 매핑하고, `GlobalExceptionHandler`가 모든 예외를 같은 형식의 `ErrorResponse`로 변환한다.
- **관리자 전용 코드는 도메인 패키지가 아니라 `admin.{domain}`에 모은다.** 여러 도메인이 공유하는 Entity/Repository/ErrorCode는 그대로 도메인 패키지에 두고 admin 쪽에서 참조하는 단방향 의존만 허용해, 일반 API와 관리자 API의 책임을 분리한다.

## 프로젝트 구조

패키지는 계층형이 아닌 **도메인 우선** 구조를 따른다. 각 도메인 아래 `controller/service/repository/dto/entity/exception` 등을 둔다.

```text
src/main/java/com/bop/youthpick/
├── admin       # 관리자 기능 (정책/사용자/게시글/로그 관리)
├── auth        # 로그인/인증, JWT 발급, OAuth(Google/Naver/Kakao) 연동
├── board       # 게시판(커뮤니티) 도메인
├── file        # 파일 업로드 (MinIO 연동)
├── global      # 공통 설정, ApiResponse/ErrorResponse, 예외 처리
├── inquiry     # 문의/고객센터
├── log         # 로그인 이력, 애플리케이션 로그
├── policy      # 청년 정책 데이터, 검색, 추천 (핵심 도메인)
├── post        # 정책 신청 관리(트래커) 게시물
├── sync        # 온통청년(youthcenter) 공공 API 배치 동기화
└── user        # 사용자 계정, 프로필, 온보딩
```

상세 아키텍처/컨벤션은 [`AGENTS.md`](./AGENTS.md)와 [`.claude/rules/`](./.claude/rules)를 참고한다.

## 시작하기

### 사전 요구사항

- JDK 21 (Gradle Toolchain이 자동 프로비저닝하므로 부트스트랩 JVM만 있으면 된다)
- Docker / Docker Compose (로컬 인프라: MySQL, Redis, MinIO)

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env`를 채운 뒤, `docker compose`는 이 값을 자동으로 읽는다. 단, **Spring Boot 애플리케이션은 `.env`를 자동으로 읽지 않으므로** DB/Redis/MinIO/JWT/OAuth 값을 앱에 전달하려면 IDE 실행 구성의 Environment variables에 등록하거나 셸에서 `export` 후 실행한다. `JWT_SECRET`은 fallback이 없어 값을 넣지 않으면 앱이 뜨지 않는다. 아무 설정 없이 실행하면 `application-local.yml`의 로컬 인프라 기본값(MySQL `localhost:3306/youthpick`, Redis `localhost:6379`)으로 동작한다.

### 2. 로컬 인프라 기동

```bash
docker compose up -d   # MySQL 8.4, Redis 7, MinIO
```

### 3. 애플리케이션 실행

백엔드 자체는 컨테이너화하지 않는다. IDE 또는 Gradle로 직접 실행한다.

```bash
./gradlew bootRun        # macOS / Linux
.\gradlew.bat bootRun     # Windows (PowerShell)
```

기본 포트는 `8080`, 기본 프로파일은 `local`이다.

### 4. 테스트

```bash
./gradlew test            # macOS / Linux / CI
.\gradlew.bat test         # Windows (PowerShell)
```

테스트는 별도 인프라 없이 H2 in-memory(MySQL 모드)로 동작한다.

### 5. 코드 포맷

```bash
./gradlew spotlessApply   # 포맷 적용
./gradlew spotlessCheck   # 포맷 검증 (test/build에는 포함되지 않음)
```

## 문서

| 문서 | 내용 |
| --- | --- |
| [`AGENTS.md`](./AGENTS.md) | 모든 에이전트/개발자 공통 진입점 — 개요, 검증 명령, 규칙 요약, 규칙 문서 맵 |
| [`docs/README.md`](./docs/README.md) | 문서 전체 인덱스 |
| [`docs/api-spec.md`](./docs/api-spec.md) | 프론트 연동 기준 API 명세 |
| [`.claude/rules/`](./.claude/rules) | 아키텍처·API 설계·에러 처리·JPA·Lombok·인증/보안·테스트·인프라 규칙 (주제별 문서) |

## 작업 흐름

```text
GitHub Issue 생성 → 브랜치 생성 → 작업/커밋 → PR 생성 → 리뷰/검증 확인 → merge
```

- 브랜치: `feat|fix|docs|refac/{issue-number}-{short-name}`
- 커밋: `type: subject` (`feat`/`fix`/`docs`/`refac`/`test`/`chore`)
- `main` 직접 커밋 금지, 이슈 없이 임의 브랜치 작업 금지. 최소 1인 리뷰 후 merge.

자세한 내용은 [`.claude/rules/workflow.md`](./.claude/rules/workflow.md)를 참고한다.

## 관련 저장소

- Front-end: [YouthPick/front-end](https://github.com/YouthPick/front-end)
