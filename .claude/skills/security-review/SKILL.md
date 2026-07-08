---
name: security-review
description: YouthPick 백엔드 변경분의 보안 점검. 인증/세션/OAuth, secret 노출, 입력 검증, 인가, 민감정보 응답 노출을 검토한다. PR 전 또는 auth/user/secret 관련 코드를 만졌을 때 사용.
---

# Security Review

YouthPick 백엔드(세션 기반 인증 + OAuth 소셜 로그인 + Redis 세션) 변경분을 보안 관점에서 점검한다.

## 절차

1. 리뷰 범위를 정한다. 기본은 현재 브랜치의 diff:
   ```bash
   git diff --merge-base main
   ```
2. [`checklist.md`](./checklist.md)의 각 항목을 diff에 대입해 점검한다.
3. 발견 항목을 **심각도(critical / high / medium / low)** 순으로 정리한다. 각 항목에 `파일:라인`, 무엇이 문제인지, 어떻게 고칠지를 적는다.
4. 확실하지 않은 지적은 "가능성"으로 표시하고 근거를 남긴다. 추측을 확정처럼 말하지 않는다.

## 원칙

- secret / OAuth client secret / 세션 토큰 값이 코드·로그·응답·예외 메시지에 노출되는지 최우선으로 본다.
- 규칙 근거는 [`.claude/rules/auth-security.md`](../../rules/auth-security.md), [`.claude/rules/error-handling.md`](../../rules/error-handling.md)와 일치시킨다.
- 이 스킬은 **점검·보고만** 한다. 수정이 필요하면 항목을 제시하고, 실제 수정은 사용자 승인 후 진행한다.
