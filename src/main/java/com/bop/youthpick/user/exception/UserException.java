package com.bop.youthpick.user.exception;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.global.error.GlobalErrorCode;

public class UserException extends CustomException {

    public UserException(GlobalErrorCode errorCode) {
        super(errorCode);
    }
}
