# CLAUDE.md

Claude Code 또는 유사한 코딩 에이전트는 이 레포에서 작업할 때 반드시 다음 문서를 먼저 읽고 그대로 따른다.

- 백엔드 구현 규칙: [`docs/rules.md`](./docs/rules.md)
- Git 워크플로: [`docs/git-convention.md`](./docs/git-convention.md)
- 문서 전체 목록/컨벤션: [`docs/README.md`](./docs/README.md)

## Non-negotiable rules

- 코드 변경 전에 [`docs/rules.md`](./docs/rules.md)를 먼저 읽는다.
- 패키지는 도메인 우선 + 단순 구조를 유지한다: `com.bop.youthpick.{domain}.controller/service/repository/dto/entity`.
- DDD식 `{domain}.api / application / domain / infrastructure / external` 패키지를 만들지 않는다.
- 요청 DTO에 Bean Validation을 적용하고, `@RequestBody`에는 `@Valid`를 붙인다.
- JPA Entity를 Controller에서 직접 반환하지 않는다.
- 성공 응답은 `global.common.ApiResponse`로 감싸고, 페이지 등 부가정보는 `meta`에 담는다.
- 예외는 `global.error`의 `ErrorCode` + `CustomException` 기반 공통 처리를 사용한다. 비즈니스 에러에 raw `RuntimeException`을 던지지 않는다.
- 목록 API는 Spring Data `Pageable`/`Page`를 사용한다. `page`/`size`/`totalPages`를 직접 계산하지 않는다.
- Lombok은 보일러플레이트 축소 용도로만: `@RequiredArgsConstructor`, `@Getter`, JPA `@NoArgsConstructor(access = AccessLevel.PROTECTED)`만 허용. `@Data`, Entity `@Setter`, Entity `@AllArgsConstructor`는 금지.
- secret / OAuth client secret / token 값을 코드·로그·응답에 노출하지 않는다. `.env`는 커밋하지 않는다.
- 완료를 주장하기 전에 테스트를 실행한다.

## 기술 스택 요약

- Java 21 · Spring Boot 3.5.16 · Gradle(Groovy DSL, `build.gradle`)
- Spring Web MVC / Data JPA / Security / Session(Redis) / Validation / Actuator
- DB: 로컬·테스트 H2 in-memory(MySQL 모드), 배포 MySQL
- 인증: Spring Security 세션 기반 + OAuth 소셜 로그인(Google/Naver/Kakao), 세션은 Redis 저장

## Git workflow

전체 규칙은 [`docs/git-convention.md`](./docs/git-convention.md)를 따른다. 핵심:

```text
GitHub Issue → 브랜치 생성 → 작업/커밋 → PR → 리뷰/검증 → merge
```

- `main`에 직접 커밋하지 않는다. 이슈 없이 임의 브랜치에서 작업하지 않는다.
- 브랜치: `feat|fix|docs|refac/{issue-number}-{short-name}` (기본 분기: `dev` 있으면 `dev`, 없으면 `main`)
- 커밋: `type: subject` (`feat|fix|docs|refac|test|chore`)
- PR 본문: 변경 내용 · 테스트 결과 · `Closes #이슈번호`

## Required verification

테스트를 실행해 통과를 확인한 뒤에만 완료를 주장한다. Java toolchain이 JDK 21을 자동 프로비저닝하므로 `JAVA_HOME`을 수동 설정할 필요는 없다.

```powershell
# Windows (PowerShell)
.\gradlew.bat test
```

```bash
# macOS / Linux / CI
./gradlew test
```

Docker/Compose(인프라: Redis, 필요 시 MySQL) 관련 변경 시 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. 실행 환경에 Docker가 없으면 PR 검증 결과에 명시한다. (이 레포는 앱을 컨테이너화하지 않고 IDE/Gradle로 실행하며, compose는 로컬 인프라만 띄운다.)