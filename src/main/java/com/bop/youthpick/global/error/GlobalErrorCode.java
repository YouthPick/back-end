package com.bop.youthpick.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 코드 카탈로그.
 *
 * <p>각 코드는 HTTP 상태 + 서비스 코드(접두어 + 3자리) + 사용자 메시지를 함께 가진다.
 * 접두어: 공통 C, 인증 A, 서버 S, 사용자 U, 정책 P, 즐겨찾기 F, 자가진단 D.
 *
 * <p>새 에러는 반드시 여기에 추가하고 {@code throw new CustomException(ErrorCode.XXX)}
 * (혹은 {@link ResourceNotFoundException}/{@link ConflictException})로 사용한다.
 * 컨트롤러/서비스에서 메시지 문자열을 직접 만들지 않는다.
 */
@Getter
public enum GlobalErrorCode {


    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부에 예기치 않은 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "입력 형태가 올바르지 않습니다."),

    // --- 인증/인가 ---
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // --- 정책(policy) ---
    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "일치하는 정책이 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code; // 임의의 코드를 정하세요.
    private final String message;

    GlobalErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
