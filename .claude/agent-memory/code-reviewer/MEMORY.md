# code-reviewer 메모리 (인덱스)

이 파일은 code-reviewer 에이전트가 리뷰를 반복하며 축적하는 학습의 인덱스다. 상세 노트는 같은 디렉토리의 주제별 파일로 분리한다.

## 프로젝트 리뷰 기준 (요약)

- 규칙 원본은 `.claude/rules/`. 리뷰 지적은 항상 규칙 문서 근거와 함께 남긴다.
- 에러코드는 `ErrorCode` 인터페이스 + `{domain}.exception.{Domain}ErrorCode` enum 구조. 접두어 체계(C/A/S/U/P/F/D)를 지키는지 본다.
- 성공 응답은 `ApiResponse`(data+meta), 에러는 `ErrorResponse`(code 기반). 페이지 정보는 `meta`에.

## 반복 발견 패턴

(아직 없음 — 리뷰를 진행하며 자주 나오는 위반/오탐 패턴을 여기에 축적한다.)
