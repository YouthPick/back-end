package com.bop.youthpick.global.config;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.global.error.ErrorCode;
import com.bop.youthpick.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 됐지만 권한(role)이 부족해 거부된 요청을 처리한다(예: 일반 회원이 관리자 전용 API 접근).
 *
 * <p>{@link RestAuthenticationEntryPoint}와 동일한 이유로, 필터 단계의 거부이므로 {@code GlobalExceptionHandler}가 잡지
 * 못한다. 여기서 직접 403 + {@link ErrorResponse} JSON을 응답한다.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorCode errorCode = AuthErrorCode.FORBIDDEN;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
