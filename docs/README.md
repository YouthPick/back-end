# YouthPick 문서 (docs)

`youth-pick` 백엔드의 문서 안내다. 코드/문서 작업 전에 관련 문서를 먼저 읽는다.

## 규칙 문서 위치

개발 규칙·컨벤션의 단일 출처(SSOT)는 [`.claude/rules/`](../.claude/rules)다. 주제별 파일로 나뉘어 있고, Claude Code 등 코딩 에이전트가 자동 로드한다. 전체 맵은 [`AGENTS.md`](../AGENTS.md#규칙-문서-맵)를 본다.

| 문서 | 내용 |
|------|------|
| [`../.claude/rules/workflow.md`](../.claude/rules/workflow.md) | 작업 흐름, Git 브랜치/커밋/PR/리뷰/merge, 완료 전 검증 |
| [`../.claude/rules/architecture.md`](../.claude/rules/architecture.md) | 패키지 구조, 계층 책임, 금지 구조 |
| [`../.claude/rules/api-design.md`](../.claude/rules/api-design.md) | Controller, DTO, Validation, `ApiResponse`, Pageable |
| [`../.claude/rules/error-handling.md`](../.claude/rules/error-handling.md) | `ErrorCode`/`CustomException`/`ErrorResponse`, 에러코드 접두어 체계 |
| [`../.claude/rules/entity-jpa.md`](../.claude/rules/entity-jpa.md) | Entity/Repository/JPA |
| [`../.claude/rules/service.md`](../.claude/rules/service.md) | Service 계층, 트랜잭션 |
| [`../.claude/rules/lombok.md`](../.claude/rules/lombok.md) | Lombok 허용/금지 |
| [`../.claude/rules/auth-security.md`](../.claude/rules/auth-security.md) | 인증/세션/Redis, secret 취급 |
| [`../.claude/rules/testing.md`](../.claude/rules/testing.md) | 테스트 규칙 |
| [`../.claude/rules/infra.md`](../.claude/rules/infra.md) | Docker/Compose, API 수동 검증 |

## 설계/명세 문서

| 문서 | 내용 |
|------|------|
| [`api-spec.md`](./api-spec.md) | 프론트 연동 기준 API 명세(엔드포인트, 메서드, 권한, 파라미터) |

## 진입점 문서 (레포 루트)

| 문서 | 내용 |
|------|------|
| [`../AGENTS.md`](../AGENTS.md) | 모든 에이전트/개발자 공통 진입점. 개요 · 검증 명령 · 비협상 규칙 요약 · 규칙 문서 맵 |
| [`../CLAUDE.md`](../CLAUDE.md) | Claude Code 진입점. `AGENTS.md`를 import하고 Claude 전용 안내만 추가 |

## 협업 템플릿 (`.github/`)

| 파일 | 내용 |
|------|------|
| [`../.github/ISSUE_TEMPLATE/task.md`](../.github/ISSUE_TEMPLATE/task.md) | 작업 이슈 템플릿 (배경 / 작업 범위 / 검증) |
| [`../.github/PULL_REQUEST_TEMPLATE.md`](../.github/PULL_REQUEST_TEMPLATE.md) | PR 템플릿 (변경 내용 / 검증 / 연결 이슈) |

## 문서 컨벤션

- **위치**: 규칙·컨벤션은 `.claude/rules/`(주제별 1파일). 에이전트 진입점(`CLAUDE.md`, `AGENTS.md`)은 레포 루트. 설계·명세 등 그 외 문서는 `docs/` 아래.
- **파일명**: 소문자 + 하이픈(`kebab-case`), 확장자 `.md`.
- **언어**: 본문은 한국어. 코드 식별자/명령/예약어는 원문 그대로.
- **단일 출처(SSOT)**: 같은 규칙을 여러 문서에 복붙하지 않는다. 상세는 한 문서에 두고 나머지는 링크한다.
- **갱신**: 규칙을 바꾸면 `AGENTS.md`의 요약·규칙 문서 맵과 이 목록도 함께 갱신한다.
- **경로 스코프**: `.claude/rules/` 파일은 필요하면 YAML frontmatter의 `paths`(glob)로 적용 범위를 제한한다. `paths`가 없으면 항상 로드된다.

## 앞으로 추가하면 좋은 문서 (제안)

- `error-codes.md` — `ErrorCode` 카탈로그와 프론트 메시지 매핑
- `oauth-setup.md` — Google/Naver/Kakao OAuth 앱 등록 및 환경변수 세팅 가이드
- `local-setup.md` — 로컬 실행(H2/Redis/compose) 온보딩 가이드
