package com.bop.youthpick.global.config;

import com.bop.youthpick.global.error.GlobalErrorCode;
import com.bop.youthpick.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 요청이 보호된 엔드포인트에 닿았을 때 호출되는 진입 거부 처리기.
 *
 * <p>Spring Security 필터 단계에서 인증 실패(토큰 없음/만료 등)가 발생하면, 요청은 컨트롤러에
 * 도달하지 못하므로 {@code GlobalExceptionHandler}(@RestControllerAdvice)가 잡지 못한다.
 * 그래서 여기서 직접 401 + {@link ErrorResponse} JSON을 응답 스트림에 써넣어,
 * 컨트롤러 층의 에러 응답과 동일한 포맷을 유지한다.
 *
 * <p>기본 동작은 Security가 HTML 로그인 페이지로 리다이렉트하는 것인데, REST API에서는
 * 그게 아니라 일관된 JSON 에러가 필요하므로 이 클래스를 SecurityConfig에 등록한다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        GlobalErrorCode errorCode = GlobalErrorCode.UNAUTHORIZED;
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }
}
