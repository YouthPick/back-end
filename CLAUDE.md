@AGENTS.md

## Claude Code 안내

- 상세 규칙은 `.claude/rules/`에서 자동 로드된다. 항상 로드되는 규칙(`workflow.md`)과 경로 매칭 시 로드되는 규칙(`paths` frontmatter)으로 나뉜다. 규칙을 바꾸면 `AGENTS.md`의 규칙 문서 맵도 함께 갱신한다.
- 완료를 주장하기 전에 `./gradlew test`를 실행해 통과를 확인한다.
- 머신별 환경 메모(JDK 경로 등)는 커밋되지 않는 `CLAUDE.local.md`에 둔다.
