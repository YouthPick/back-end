# 작업 흐름 / Git 컨벤션

```text
GitHub Issue 생성 → 브랜치 생성 → 작업/커밋 → PR 생성 → 리뷰/검증 확인 → merge
```

- `main`에 직접 커밋하지 않는다. 이슈 없이 임의 브랜치에서 작업하지 않는다.
- 기획 변경이나 구현 계획이 필요한 작업은 먼저 계획을 정리하고 관련 GitHub Issue를 만든 뒤 작업한다.
- 단순 설명이나 스텁으로 끝내지 않는다. 동작하는 코드와 검증 결과를 남긴다.

## 브랜치

- `main`: 배포 가능한 안정 버전. `dev`: 팀 통합 브랜치(있으면 `dev`에서 분기, 없으면 `main`).
- 네이밍: `feat|fix|docs|refac/{issue-number}-{short-name}` — 예: `feat/12-social-login`, `fix/18-search-empty-state`

## 커밋

`type: subject` 형식. 타입: `feat`(기능) · `fix`(버그) · `docs`(문서) · `refac`(구조 개선) · `test`(테스트) · `chore`(설정/빌드).

```text
feat: 소셜 로그인 콜백 API 추가
refac: 정책 검색 서비스 책임 분리
```

## PR / 리뷰 / Merge

- PR 본문([템플릿](../../.github/PULL_REQUEST_TEMPLATE.md)): 변경 내용 · 실제 검증 결과(실행한 명령·확인 내용) · `Closes #이슈번호`
- 새 `ErrorCode`를 추가했다면 프론트 메시지 매핑 필요 여부를 PR에 명시한다.
- 리뷰 관점: User Story/AC 일치, A(정상)/E(예외)/X(경계) 케이스, 권한·빈 상태·오류·외부 장애 처리, 민감정보 노출 여부.
- 최소 1인 리뷰 후 merge. 작은 기능 단위로 PR을 나누고, merge 후 작업 브랜치는 삭제한다.

## 완료 전 검증

- 기능 변경 시 관련 작은 테스트를 먼저 실행하고, 마지막에 전체 테스트를 실행한다.
- `./gradlew test`(Windows: `.\gradlew.bat test`)를 통과시킨 뒤에만 완료를 주장한다.
- Java 코드를 작성/수정했다면 `./gradlew spotlessApply`로 포맷을 맞추고 `./gradlew spotlessCheck`(또는 `test`에 포함된 빌드)가 통과하는지 확인한다(코드 포맷 기준은 [`code-style.md`](./code-style.md)).
- Docker/Compose 관련 변경 시 가능한 범위로 `docker compose config`와 기동 smoke를 확인한다. 실행 환경에 Docker가 없으면 PR 검증 결과에 명시한다.
