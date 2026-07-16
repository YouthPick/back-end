package com.bop.youthpick.sync.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 배치 관리자 API 에러코드. 접두어 SY(sync)는 error-handling.md 표에 없어 PR 본문에서 추가를 제안한다. */
@Getter
@RequiredArgsConstructor
public enum SyncErrorCode implements ErrorCode {
    SYNC_ALREADY_RUNNING("SY001", "정책 수집이 이미 실행 중입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
