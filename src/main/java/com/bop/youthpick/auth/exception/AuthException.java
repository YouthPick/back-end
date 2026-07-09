package com.bop.youthpick.auth.exception;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.global.error.ErrorCode;

public class AuthException extends CustomException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
