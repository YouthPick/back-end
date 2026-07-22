package com.bop.youthpick.post.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoardErrorCode implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "게시글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B002", "작성자만 게시글을 수정하거나 삭제할 수 있습니다."),
    POLICY_REQUIRED(HttpStatus.BAD_REQUEST, "B003", "질문과 후기에는 정책 연결이 필요합니다."),
    FREE_POST_POLICY_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "B004", "자유글에는 정책을 연결할 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B005", "댓글을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
