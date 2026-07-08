package com.bop.youthpick.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
  HttpStatus getStatus();

  String getCode(); // 임의의 코드를 정하세요.

  String getMessage();
}
