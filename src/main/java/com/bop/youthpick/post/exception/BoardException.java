package com.bop.youthpick.post.exception;

import com.bop.youthpick.global.error.CustomException;

public class BoardException extends CustomException {

    public BoardException(BoardErrorCode errorCode) {
        super(errorCode);
    }
}
