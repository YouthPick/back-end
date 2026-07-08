package com.bop.youthpick.auth.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
