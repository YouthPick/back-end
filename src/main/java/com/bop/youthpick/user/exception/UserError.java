package com.bop.youthpick.user.exception;

import com.bop.youthpick.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserError implements ErrorCode {
    // --- 사용자(user) ---
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다."),
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "U003", "이미 온보딩 프로필이 등록되어 있습니다."),
    // 프로필 수정(PATCH)과 맞춤정책 조회(RecommendedPolicyService) 양쪽에서 쓴다 — 둘 다 "온보딩 프로필이 아직 없음"이라
    // 같은 코드로 통일한다.
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "U004", "온보딩 프로필을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
