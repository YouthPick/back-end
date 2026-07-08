package com.bop.youthpick.global.error;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 예외. 서비스 계층에서 던지고 {@link GlobalExceptionHandler}가 {@link ErrorResponse}로 변환한다.
 *
 * <p>사용: {@code throw new CustomException(ErrorCode.USER_NOT_FOUND);} 의미가 분명한 경우 {@link
 * ResourceNotFoundException}/{@link ConflictException}을 사용한다. 서비스에서 raw {@code RuntimeException}으로
 * 비즈니스 예외를 던지지 않는다.
 */
@Getter
public class CustomException extends RuntimeException {

  private final ErrorCode errorCode;

  public CustomException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
