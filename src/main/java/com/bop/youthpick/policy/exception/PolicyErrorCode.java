package com.bop.youthpick.policy.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements ErrorCode {
  POLICY_NOT_FOUND("P001", "일치하는 정책이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  POLICY_ALREADY_EXISTS("P002", "이미 존재하는 정책입니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
