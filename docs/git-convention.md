# Git 작업 컨벤션

`youth-pick`에서 코드나 문서를 수정하는 모든 사람(에이전트 포함)은 아래 흐름을 따른다.

```text
GitHub Issue 생성 → 브랜치 생성 → 작업/커밋 → PR 생성 → 리뷰/검증 확인 → merge
```

직접 `main`에 커밋하거나, 이슈 없이 임의 브랜치에서 작업하지 않는다. 애매하면 이 문서를 기준으로 맞춘다.

## 브랜치 전략

- `main`: 배포 가능한 안정 버전만 유지한다.
- `dev`: 팀 개발 통합 브랜치다. 기능 브랜치는 원칙적으로 `dev`에서 분기한다.
- `feat/*`: 신규 기능 개발 브랜치.
- `fix/*`: 버그 수정 브랜치.
- `docs/*`: 문서 작업 브랜치.
- `refac/*`: 동작 변경 없이 구조를 정리하는 브랜치.

현재 원격에 `dev` 브랜치가 없으면 `main`에서 분기하되, `dev`가 생긴 이후에는 기능 브랜치를 `dev` 기준으로 만든다.

## 브랜치 네이밍

```text
feat/{issue-number}-{short-name}
fix/{issue-number}-{short-name}
docs/{issue-number}-{short-name}
refac/{issue-number}-{short-name}
```

예시:

```text
feat/12-social-login
fix/18-search-empty-state
docs/24-api-spec
refac/31-policy-service
```

## 커밋 컨벤션

커밋 메시지는 아래 형식을 따른다.

```text
type: subject
```

예시:

```text
feat: 소셜 로그인 콜백 API 추가
fix: 검색 결과 없음 상태 처리 수정
docs: API 명세서 오류 케이스 보강
refac: 정책 검색 서비스 책임 분리
test: 관심정책 중복 방지 테스트 추가
chore: 프로젝트 설정 파일 정리
```

커밋 타입 기준:

- `feat`: 사용자에게 보이는 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `refac`: 기능 변경 없는 구조 개선
- `test`: 테스트 추가/수정
- `chore`: 설정, 빌드, 패키지 등 기타 작업

## PR 규칙

PR 본문에는 반드시 아래 내용을 적는다. ([PR 템플릿](../.github/PULL_REQUEST_TEMPLATE.md))

- 변경 내용
- 테스트 결과
- 필요한 경우 스크린샷 또는 로그
- 연결 이슈: `Closes #이슈번호`

PR은 최소 1명 이상 리뷰 후 merge하는 것을 원칙으로 한다. 충돌 해결, 테스트 통과, 문서 반영 후 merge한다.

## 리뷰 기준

- 기획서의 User Story / Acceptance Criteria와 구현이 맞는지 확인한다.
- A(정상)/E(예외)/X(경계·비정상) 테스트 관점이 빠지지 않았는지 확인한다.
- 권한, 빈 상태, 오류 상태, 외부 장애 처리가 있는지 확인한다.
- 민감정보(secret, OAuth token 등)가 응답, 로그, 프론트 저장소에 남지 않는지 확인한다.

## Merge 규칙

- 작은 기능 단위로 PR을 나눈다. 큰 PR은 피한다.
- merge 후 `feat/*`, `fix/*`, `docs/*`, `refac/*` 브랜치는 삭제한다.
- 배포 영향이 있는 변경은 이슈/문서에 변경 내용을 남긴다.