# AGENTS.md

이 레포에서 작업하는 모든 에이전트는 작업 전에 반드시 [`docs/rules.md`](./docs/rules.md)와 [`docs/git-convention.md`](./docs/git-convention.md)를 읽고 따른다. (Claude Code는 [`CLAUDE.md`](./CLAUDE.md)도 함께 참고한다.)

## 필수 작업 규칙

1. [`docs/rules.md`](./docs/rules.md)를 현재 작업의 최우선 백엔드 구현 규칙으로 사용한다.
2. `docs/rules.md`와 기존 코드가 충돌하면, 무작정 기존 패턴을 복사하지 말고 `docs/rules.md` 기준으로 정리한다.
3. 패키지는 도메인 단위로 나누되, 도메인 하위는 `controller`, `service`, `repository`, `dto`, `entity` 단순 구조를 유지한다.
4. 요청 DTO에는 Bean Validation을 적용하고, `@RequestBody`에는 `@Valid`를 붙인다.
5. 목록 API는 직접 `page`/`size`를 구현하지 말고 Spring Data `Pageable`/`Page`를 사용한다.
6. 성공 응답은 `global.common.ApiResponse`로 감싸고, 페이지 등 부가정보는 `meta`에 담는다.
7. 예외 처리는 `global.error`의 `ErrorCode` + `CustomException` 기반 공통 예외 처리 구조를 따른다.
8. Entity를 API 응답으로 직접 반환하지 않는다.
9. secret / OAuth token / 민감정보를 로그나 응답에 노출하지 않는다. `.env`는 커밋하지 않는다.
10. Lombok은 `@RequiredArgsConstructor`, `@Getter`, JPA `@NoArgsConstructor(access = AccessLevel.PROTECTED)`처럼 보일러플레이트 축소 용도로만 사용하고, `@Data`/Entity `@Setter`/Entity `@AllArgsConstructor`는 사용하지 않는다.
11. 변경 후 관련 작은 테스트를 먼저 실행하고, 마지막에 전체 테스트를 실행한다.

## Git 작업 컨벤션

전체 규칙은 [`docs/git-convention.md`](./docs/git-convention.md)를 따른다. 코드/문서를 수정할 때 반드시 아래 흐름을 지킨다.

```text
GitHub Issue 생성 → 브랜치 생성 → 작업/커밋 → PR 생성 → 리뷰/검증 확인 → merge
```

직접 `main`에 커밋하거나, 이슈 없이 임의 브랜치에서 작업하지 않는다.

- 브랜치: `feat|fix|docs|refac/{issue-number}-{short-name}`
- 커밋: `type: subject` (`feat|fix|docs|refac|test|chore`)
- PR 본문: 변경 내용 · 테스트 결과 · `Closes #이슈번호`

## 검증 명령

```powershell
# Windows (PowerShell)
.\gradlew.bat test
```

```bash
# macOS / Linux / CI
./gradlew test
```

Java toolchain이 JDK 21을 자동 프로비저닝하므로 `JAVA_HOME`을 수동으로 지정할 필요는 없다. Docker/Compose(로컬 인프라: Redis, 필요 시 MySQL) 관련 변경은 가능한 범위로 `docker compose config`와 기동을 확인하고, Docker가 없으면 PR 검증 결과에 명시한다.