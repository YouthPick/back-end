---
description: GitHub 이슈를 Git 컨벤션대로 브랜치 생성부터 검증까지 처리한다
argument-hint: <이슈번호>
allowed-tools: Bash(git checkout:*), Bash(git switch:*), Bash(git branch:*), Bash(git add:*), Bash(git commit:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(gh issue view:*), Bash(gh pr create:*), Bash(./gradlew test:*), Read, Edit, Write, Grep, Glob
---

이슈 #$1 을 이 레포의 Git 컨벤션([.claude/rules/workflow.md](../rules/workflow.md))에 따라 처리한다.

1. `gh issue view $1` 로 이슈 내용·작업 범위·검증 기준을 파악한다.
2. 기본 분기(`dev` 있으면 `dev`, 없으면 `main`)에서 `type/$1-{short-name}` 브랜치를 만든다. type은 이슈 성격에 맞춰 `feat|fix|docs|refac` 중 고른다.
3. 관련 규칙 파일(`.claude/rules/`)을 먼저 읽고, 규칙을 지키며 구현한다.
4. 관련 작은 테스트 → 전체 `./gradlew test` 순으로 검증한다. (JAVA_HOME 필요 시 `CLAUDE.local.md` 참고)
5. `type: subject` 커밋을 만들고, 변경 내용·검증 결과·`Closes #$1` 을 담은 PR 본문 초안을 제시한다.

파괴적이거나 이슈 범위를 벗어나는 결정이 필요하면 진행 전에 사용자에게 확인한다.
