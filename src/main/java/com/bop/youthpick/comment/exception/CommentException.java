package com.bop.youthpick.comment.exception;

import com.bop.youthpick.global.error.CustomException;

public class CommentException extends CustomException {

    public CommentException(CommentErrorCode errorCode) {
        super(errorCode);
    }
}
