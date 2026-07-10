# YouthPick Back-end — Agent Guide

모든 코딩 에이전트(그리고 새로 합류한 개발자)의 공통 진입점이다. 상세 규칙은 [`.claude/rules/`](./.claude/rules)에 주제별로 나뉘어 있다. Claude Code는 이 디렉토리를 자동 로드하고, 다른 에이전트는 아래 규칙 문서 맵을 보고 작업 관련 파일을 먼저 읽는다.

## 프로젝트 개요

- 청년 정책 추천 서비스 **YouthPick** 백엔드 API 서버
- Java 21 · Spring Boot 3.5.16 · Gradle Wrapper(Groovy DSL, `build.gradle`)
- Spring Web MVC / Data JPA / Security / Session(Redis) / Validation / Actuator
- DB: 로컬·배포 MySQL + Flyway(`db/migration`, JPA는 validate만) / 테스트만 H2 in-memory(MySQL 모드) — 프로파일/환경변수로 전환(기본 프로파일 `local`)
- 인증: Spring Security 세션 기반 + OAuth 소셜 로그인(Google/Naver/Kakao), 세션은 Redis 저장
- 패키지 루트: `com.bop.youthpick`

## 검증 명령

완료를 주장하기 전에 테스트를 실행해 통과를 확인한다. Java toolchain이 JDK 21을 자동 프로비저닝하므로 부트스트랩 JVM만 있으면 된다.

```bash
# macOS / Linux / CI
./gradlew test
```

```powershell
# Windows (PowerShell)
.\gradlew.bat test
```

로컬 인프라(Redis, 필요 시 MySQL)는 `docker compose up -d`로 띄운다. 앱 자체는 컨테이너화하지 않고 IDE/Gradle로 실행한다.

## Non-negotiable rules (요약)

- `main` 직접 커밋 금지, 이슈 없이 임의 브랜치 작업 금지. 브랜치는 `feat|fix|docs|refac/{issue-number}-{short-name}`.
- 패키지는 도메인 우선 + 단순 구조: `com.bop.youthpick.{domain}.controller/service/repository/dto/entity/exception`. DDD식 `api/application/domain/infrastructure/external` 금지.
- 요청 DTO에 Bean Validation 적용, `@RequestBody`에는 `@Valid`. Entity를 Controller에서 직접 반환하지 않는다.
- 성공 응답은 `global.common.ApiResponse`(data + meta), 에러 응답은 `global.error.ErrorResponse`(code 기반) 공통 처리. 비즈니스 에러에 raw `RuntimeException` 금지.
- 목록 API는 Spring Data `Pageable`/`Page` 사용. `page`/`size`/`totalPages` 직접 계산 금지.
- Lombok은 `@RequiredArgsConstructor`, `@Getter`, JPA `@NoArgsConstructor(access = PROTECTED)`만. `@Data`, Entity `@Setter`/`@AllArgsConstructor` 금지.
- secret / OAuth client secret / token 값을 코드·로그·응답에 노출하지 않는다. `.env`는 커밋하지 않는다.

## 규칙 문서 맵

| 문서 | 내용 | 적용 범위 |
|------|------|-----------|
| [`.claude/rules/workflow.md`](./.claude/rules/workflow.md) | 작업 흐름, Git 브랜치/커밋/PR/리뷰, 완료 전 검증 | 항상 |
| [`.claude/rules/code-style.md`](./.claude/rules/code-style.md) | 코드 포맷(Google Java Style, AOSP), Spotless | `src` |
| [`.claude/rules/architecture.md`](./.claude/rules/architecture.md) | 패키지 구조, 계층 책임, 금지 구조 | `src/main/java` |
| [`.claude/rules/api-design.md`](./.claude/rules/api-design.md) | Controller, DTO, Validation, `ApiResponse`, Pageable | `src/main/java` |
| [`.claude/rules/error-handling.md`](./.claude/rules/error-handling.md) | `ErrorCode` 인터페이스, 도메인 에러코드, `ErrorResponse` | `src/main/java` |
| [`.claude/rules/entity-jpa.md`](./.claude/rules/entity-jpa.md) | Entity/Repository/JPA 규칙 | `src/main/java` |
| [`.claude/rules/service.md`](./.claude/rules/service.md) | Service 계층, 트랜잭션 | `src/main/java` |
| [`.claude/rules/lombok.md`](./.claude/rules/lombok.md) | Lombok 허용/금지 목록 | `src/main/java` |
| [`.claude/rules/auth-security.md`](./.claude/rules/auth-security.md) | 인증/세션/Redis, secret 취급 | `src/main/java`, `resources` |
| [`.claude/rules/testing.md`](./.claude/rules/testing.md) | 테스트 작성/실행 규칙 | `src/test` |
| [`.claude/rules/infra.md`](./.claude/rules/infra.md) | Docker/Compose, 프로파일, API 수동 검증 | compose·resources |

문서 컨벤션과 전체 문서 목록은 [`docs/README.md`](./docs/README.md)를 본다.
