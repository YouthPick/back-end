package com.bop.youthpick.global.error;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공통 에러 응답 계약. 프론트가 {@code code} 기준으로 메시지를 매핑할 수 있도록 모든 에러 응답은 {@code code}를 필수로 포함한다. validation
 * 실패 시 {@code errors}에 필드 단위 상세가 담긴다. 예시보고 이해하시면 됩니다. POST /api/v1/users body:
 * {"username":"hong","password":"","email":"hong@a.com"} { "status": 400, "code": "C001",
 * "message": "입력 형태가 올바르지 "errors": [ { "field": "password", "value": "", "reason": "비밀번호는 필수입니다."
 * } ], "timestamp": "2026-06-30T14:13:40.005" }
 *
 * <p>GET /api/v1/users/999 { "status": 404, "code": "U001", "message": "사용자를 찾을 수 없습니다.", "errors":
 * [], "timestamp": "2026-06-30T14:16:00.900" }
 */
public record ErrorResponse(
    int status,
    String code,
    String message,
    List<FieldErrorDetail> errors,
    LocalDateTime timestamp) {
  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        errorCode.getMessage(),
        List.of(),
        LocalDateTime.now());
  }

  public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> errors) {
    return new ErrorResponse(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        errorCode.getMessage(),
        errors,
        LocalDateTime.now());
  }

  /** 내부 exception class, SQL, stack trace, secret 값은 넣지 않는다. */
  public record FieldErrorDetail(String field, String value, String reason) {}
}
