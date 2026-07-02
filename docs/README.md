# YouthPick 문서 (docs)

`youth-pick` 백엔드의 개발 규칙과 컨벤션 문서 모음이다. 코드/문서 작업 전에 관련 문서를 먼저 읽는다.

## 문서 목록

| 문서 | 내용 |
|------|------|
| [`rules.md`](./rules.md) | 백엔드 구현 규칙 (패키지 구조, Controller/DTO/Validation, 예외 처리, Lombok, Entity/JPA, Service, Pageable, 인증/세션/Redis, Docker, 테스트) |
| [`git-convention.md`](./git-convention.md) | Git 워크플로, 브랜치 전략/네이밍, 커밋 컨벤션, PR/리뷰/merge 규칙 |

레포 루트에는 에이전트 진입점 문서가 있다.

| 문서 | 내용 |
|------|------|
| [`../CLAUDE.md`](../CLAUDE.md) | Claude Code 등 코딩 에이전트가 자동 로드하는 진입점. 비협상 규칙 요약 + 상세 문서 링크 |
| [`../AGENTS.md`](../AGENTS.md) | 에이전트 필수 규칙 요약 (범용 에이전트 진입점) |

`.github/` 에는 협업 템플릿이 있다.

| 파일 | 내용 |
|------|------|
| [`../.github/ISSUE_TEMPLATE/task.md`](../.github/ISSUE_TEMPLATE/task.md) | 작업 이슈 템플릿 (배경 / 작업 범위 / 검증) |
| [`../.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md) | PR 템플릿 (변경 내용 / 검증 / 연결 이슈) |

## 문서 컨벤션

새 문서를 추가하거나 기존 문서를 고칠 때 아래를 따른다.

- **위치**: 에이전트가 자동으로 읽어야 하는 진입점(`CLAUDE.md`, `AGENTS.md`)만 레포 루트에 둔다. 그 외 상세 규칙·설계·컨벤션 문서는 모두 `docs/` 아래에 둔다.
- **파일명**: 소문자 + 하이픈(`kebab-case`), 확장자 `.md`. 예: `git-convention.md`, `api-spec.md`.
- **언어**: 본문은 한국어. 코드 식별자/명령/예약어는 원문 그대로.
- **단일 출처(SSOT)**: 같은 규칙을 여러 문서에 복붙하지 않는다. 상세 내용은 한 문서에 두고 나머지는 링크한다. (예: Git 규칙 원본은 `git-convention.md`, `CLAUDE.md`/`AGENTS.md`는 요약 + 링크)
- **링크**: 문서 간 참조는 상대경로 링크를 사용해 최신 위치를 가리키게 한다.
- **갱신**: 규칙을 바꾸면 이 목록과 관련 진입점(`CLAUDE.md`/`AGENTS.md`)의 링크·요약도 함께 갱신한다.

## 앞으로 추가하면 좋은 문서 (제안)

- `api-spec.md` — API 명세(엔드포인트, 요청/응답, 에러코드 매핑표)
- `error-codes.md` — `ErrorCode` 카탈로그와 프론트 메시지 매핑
- `oauth-setup.md` — Google/Naver/Kakao OAuth 앱 등록 및 환경변수 세팅 가이드
- `local-setup.md` — 로컬 실행(H2/Redis/compose) 온보딩 가이드