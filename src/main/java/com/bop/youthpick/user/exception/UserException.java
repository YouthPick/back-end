package com.bop.youthpick.user.exception;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.global.error.ErrorCode;

public class UserException extends CustomException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
