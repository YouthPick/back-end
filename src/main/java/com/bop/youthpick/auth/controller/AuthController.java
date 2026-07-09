package com.bop.youthpick.auth.controller;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.dto.AuthUserResponse;
import com.bop.youthpick.auth.dto.OAuthAuthorizationUrlResponse;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/oauth/{provider}/authorization-url")
    public ApiResponse<OAuthAuthorizationUrlResponse> authorizationUrl(
            @PathVariable String provider, HttpSession session) {
        String url = authService.buildAuthorizationUrl(provider, session);
        return ApiResponse.ok(new OAuthAuthorizationUrlResponse(url));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.login(provider, code, state, request, response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authService.frontendRedirectUri()))
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = authService.getCurrentUser(principal.userId());
        return ApiResponse.ok(AuthUserResponse.from(user));
    }
}
