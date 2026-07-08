---
paths:
  - "src/main/java/**"
---

# 예외 처리

공통 기반은 `global.error`, 도메인별 에러코드·예외는 `{domain}.exception`에 둔다.

## 구조

- `global.error.ErrorCode` — **인터페이스** (`getStatus()`, `getCode()`, `getMessage()`)
- `global.error.CustomException` — `ErrorCode`를 담는 비즈니스 예외 베이스
- `global.error.ErrorResponse` — 공통 에러 응답 (`status`, `code`, `message`, `errors[]`, `timestamp`)
- `global.error.GlobalExceptionHandler` — 모든 컨트롤러 예외를 `ErrorResponse`로 변환. validation 실패는 `C001`, 예상 못 한 예외는 `S001`로 처리한다.
- `{domain}.exception.{Domain}ErrorCode` — `ErrorCode`를 구현하는 enum (예: `AuthErrorCode`, `PolicyErrorCode`)
- `{domain}.exception.{Domain}Exception` — 필요 시 `CustomException`을 상속한 도메인 예외 (예: `UserException`)

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## 서비스 코드 규칙 — 접두어 + 3자리

| 접두어 | 영역 | | 접두어 | 영역 |
|--------|------|-|--------|------|
| `C` | 공통(common) | | `U` | 사용자(user) |
| `A` | 인증/인가(auth) | | `P` | 정책(policy) |
| `S` | 서버(server) | | `F` | 즐겨찾기(favorite) |
| | | | `D` | 자가진단(diagnosis) |

## 규칙

- 비즈니스 예외는 `throw new CustomException(에러코드)` 또는 도메인 예외(`UserException` 등)로 던진다. raw `RuntimeException` 금지.
- Controller에서 `try-catch`로 에러 응답을 직접 만들지 않는다. `GlobalExceptionHandler`에 위임한다.
- 내부 예외 메시지/SQL/stack trace/secret 값을 응답에 노출하지 않는다. `errors[].reason`에는 사용자에게 보여줄 수 있는 검증 메시지만 넣는다.
- `errors[].value`에는 거절된 입력값(rejectedValue)이 담긴다. 비밀번호·토큰 등 **민감 필드는 `value`를 비우거나 마스킹한다.** 현재 `GlobalExceptionHandler`는 rejectedValue를 그대로 담으므로, 민감 입력을 검증하는 DTO는 응답에 값이 반사되지 않도록 처리한다.
- 새 에러코드는 접두어 체계를 지키고 사용자 메시지를 함께 정의하며, 프론트 매핑 필요 여부를 PR 본문에 명시한다.
- 같은 오류에 HTTP status만 다르게 내리거나, 같은 `code`에 다른 의미를 부여하지 않는다.

## 에러 응답 형식

프론트가 `code` 기준으로 메시지를 매핑한다. validation 실패(HTTP 400 + `C001`) 예시:

```json
{
  "status": 400,
  "code": "C001",
  "message": "입력 형태가 올바르지 않습니다.",
  "errors": [
    { "field": "username", "value": "", "reason": "아이디는 필수입니다." }
  ],
  "timestamp": "2026-06-30T14:13:40.005"
}
```
