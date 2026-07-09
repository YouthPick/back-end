package com.bop.youthpick.auth.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "A003", "지원하지 않는 로그인 방식입니다."),
    INVALID_OAUTH_STATE(HttpStatus.BAD_REQUEST, "A004", "로그인 요청이 유효하지 않습니다. 다시 시도해주세요."),
    OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "A005", "소셜 로그인 서버와 통신 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
