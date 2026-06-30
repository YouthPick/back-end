package com.bop.youthpick.global.error;

/** 존재하지 않는 리소스 조회 시 사용한다. 예: {@code new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND)} */
public class ResourceNotFoundException extends CustomException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
