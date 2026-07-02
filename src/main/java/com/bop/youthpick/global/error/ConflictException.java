package com.bop.youthpick.global.error;

/** 상태 충돌, 중복 등록, 중복 실행 등에 사용한다. 예: {@code new ConflictException(ErrorCode.FAVORITE_ALREADY_EXISTS)} */
public class ConflictException extends CustomException {

    public ConflictException(GlobalErrorCode errorCode) {
        super(errorCode);
    }
}
