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
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 단위 traceId/method/uri/ip/userId를 MDC(ThreadContext)에 채운다. app_logs JDBC Appender가 이 값을 읽어 로그
 * 행을 채우므로, 인증된 userId를 읽을 수 있도록 JwtAuthenticationFilter 이후에 실행해야 한다.
 */
public class RequestLogContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ThreadContext.put("traceId", UUID.randomUUID().toString());
        ThreadContext.put("method", request.getMethod());
        ThreadContext.put("uri", request.getRequestURI());
        ThreadContext.put("ip", request.getRemoteAddr());
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
}
