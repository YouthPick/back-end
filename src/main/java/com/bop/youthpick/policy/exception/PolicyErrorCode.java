package com.bop.youthpick.policy.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements ErrorCode {
    POLICY_NOT_FOUND("P001", "일치하는 정책이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
    POLICY_ALREADY_EXISTS("P002", "이미 존재하는 정책입니다.", HttpStatus.CONFLICT),
    REGION_NOT_FOUND("P003", "존재하지 않는 지역 코드입니다.", HttpStatus.NOT_FOUND),
    POLICY_APPLICATION_NOT_FOUND("P004", "정책 관리 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CHECKLIST_NOT_FOUND("P005", "체크리스트 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    END_AT_AFTER_POLICY_DEADLINE(
            "P006", "개인 마감일은 정책 신청 마감일을 넘을 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
