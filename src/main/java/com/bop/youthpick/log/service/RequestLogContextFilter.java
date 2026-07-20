package com.bop.youthpick.log.service;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 단위 traceId/method/uri/ip/userId를 MDC(ThreadContext)에 채운다. application_logs JDBC Appender가 이 값을
 * 읽어 로그 행을 채우므로, 인증된 userId를 읽을 수 있도록 JwtAuthenticationFilter 이후에 실행해야 한다.
 */
public class RequestLogContextFilter extends OncePerRequestFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ThreadContext.put("traceId", UUID.randomUUID().toString());
        ThreadContext.put("method", request.getMethod());
        ThreadContext.put("uri", request.getRequestURI());
        ThreadContext.put("ip", clientIp(request));
        Long userId = currentUserId();
        if (userId != null) {
            ThreadContext.put("userId", String.valueOf(userId));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getPrincipal() instanceof AuthPrincipal principal
                ? principal.userId()
                : null;
    }

    /** 리버스 프록시/로드밸런서 뒤에서는 소켓 IP 대신 X-Forwarded-For의 첫 값(원 클라이언트 IP)을 우선한다. */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
