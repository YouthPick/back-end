package com.bop.youthpick.file.exception;

import com.bop.youthpick.global.error.CustomException;

public class FileException extends CustomException {

    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }
}
