package com.bop.youthpick.board.exception;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.global.error.ErrorCode;

public class BoardException extends CustomException {

    public BoardException(ErrorCode errorCode) {
        super(errorCode);
    }
}
