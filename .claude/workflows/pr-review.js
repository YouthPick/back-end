export const meta = {
  name: 'pr-review',
  description: 'YouthPick 백엔드 변경분을 관점별로 병렬 리뷰하고 각 지적을 검증한다',
  phases: [
    { title: 'Review', detail: '규칙/버그/보안/테스트 관점 병렬 리뷰' },
    { title: 'Verify', detail: '각 지적을 독립적으로 재검증' },
  ],
}

// 리뷰 관점. 각 관점은 이 레포의 규칙 문서를 근거로 삼는다.
const DIMENSIONS = [
  {
    key: 'rules',
    prompt:
      '현재 브랜치 diff(`git diff --merge-base main`)를 리뷰한다. .claude/rules/ 의 architecture.md, api-design.md, lombok.md, entity-jpa.md 를 기준으로 규칙 위반을 찾는다: 패키지 구조, 얇은 Controller, @Valid 누락, Entity 직접 반환, ApiResponse 미사용, 금지 Lombok, Pageable 미사용. 파일:라인과 근거를 반드시 남긴다.',
  },
  {
    key: 'bugs',
    prompt:
      '현재 브랜치 diff(`git diff --merge-base main`)를 리뷰한다. 정확성/버그 관점: 트랜잭션 경계(@Transactional readOnly 여부), null/Optional 처리, 경계 조건, 동시성. 파일:라인과 재현 시나리오를 남긴다.',
  },
  {
    key: 'security',
    prompt:
      '현재 브랜치 diff(`git diff --merge-base main`)를 리뷰한다. .claude/rules/auth-security.md 와 error-handling.md 기준 보안 관점: secret/토큰/세션 값 노출, 인가 누락(IDOR), 입력 검증 우회, 예외 응답의 내부정보 노출. 파일:라인과 근거를 남긴다.',
  },
  {
    key: 'tests',
    prompt:
      '현재 브랜치 diff(`git diff --merge-base main`)를 리뷰한다. .claude/rules/testing.md 기준으로 테스트 관점: 변경분에 대한 A(정상)/E(예외)/X(경계) 테스트가 있는지, 빠진 케이스는 무엇인지. 파일:라인을 남긴다.',
  },
]

const FINDINGS_SCHEMA = {
  type: 'object',
  properties: {
    findings: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low'] },
          file: { type: 'string' },
          line: { type: 'integer' },
          summary: { type: 'string' },
          suggestion: { type: 'string' },
        },
        required: ['severity', 'file', 'summary'],
      },
    },
  },
  required: ['findings'],
}

const VERDICT_SCHEMA = {
  type: 'object',
  properties: {
    isReal: { type: 'boolean' },
    reason: { type: 'string' },
  },
  required: ['isReal', 'reason'],
}

// 관점별 리뷰 → 각 지적이 나오는 즉시 독립 검증(파이프라인, 배리어 없음).
const results = await pipeline(
  DIMENSIONS,
  (d) =>
    agent(d.prompt, {
      label: `review:${d.key}`,
      phase: 'Review',
      schema: FINDINGS_SCHEMA,
    }),
  (review, d) =>
    parallel(
      (review?.findings ?? []).map((f) => () =>
        agent(
          `다음 리뷰 지적이 실제 문제인지 diff와 코드를 직접 열어 검증하라. 규칙 근거가 없거나 오탐이면 isReal=false. 지적: [${f.severity}] ${f.file}:${f.line ?? '?'} — ${f.summary}`,
          { label: `verify:${d.key}:${f.file}`, phase: 'Verify', schema: VERDICT_SCHEMA }
        ).then((v) => ({ ...f, dimension: d.key, verdict: v }))
      )
    )
)

const confirmed = results
  .flat()
  .filter(Boolean)
  .filter((f) => f.verdict?.isReal)

const order = { critical: 0, high: 1, medium: 2, low: 3 }
confirmed.sort((a, b) => (order[a.severity] ?? 9) - (order[b.severity] ?? 9))

return { confirmedCount: confirmed.length, findings: confirmed }
