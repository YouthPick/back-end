package com.bop.youthpick.comment.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B006", "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "B007", "작성자만 댓글을 수정하거나 삭제할 수 있습니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "B008", "대댓글에는 답글을 남길 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
