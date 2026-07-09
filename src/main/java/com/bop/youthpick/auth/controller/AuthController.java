package com.bop.youthpick.auth.controller;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.dto.AuthUserResponse;
import com.bop.youthpick.auth.dto.OAuthAuthorizationUrlResponse;
import com.bop.youthpick.auth.dto.OAuthCallbackRequest;
import com.bop.youthpick.auth.dto.TokenRefreshRequest;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/oauth/{provider}/authorization-url")
    public ApiResponse<OAuthAuthorizationUrlResponse> authorizationUrl(
            @PathVariable String provider) {
        String url = authService.buildAuthorizationUrl(provider);
        return ApiResponse.ok(new OAuthAuthorizationUrlResponse(url));
    }

    @PostMapping("/oauth/{provider}/callback")
    public ApiResponse<TokenResponse> callback(
            @PathVariable String provider, @Valid @RequestBody OAuthCallbackRequest request) {
        TokenResponse tokens = authService.login(provider, request.code(), request.state());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        TokenResponse tokens = authService.refresh(request.refreshToken());
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.logout(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = authService.getCurrentUser(principal.userId());
        return ApiResponse.ok(AuthUserResponse.from(user));
    }
}
