package com.bop.youthpick.auth.controller;

import com.bop.youthpick.auth.dto.AccessTokenResponse;
import com.bop.youthpick.auth.dto.AuthUserResponse;
import com.bop.youthpick.auth.dto.OAuthAuthorizationUrlResponse;
import com.bop.youthpick.auth.dto.OAuthCallbackRequest;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.auth.service.RefreshTokenCookieSupport;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.global.config.SecurityConfig;
import com.bop.youthpick.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieSupport refreshTokenCookieSupport;

    @GetMapping("/oauth/{provider}/authorization-url")
    public ApiResponse<OAuthAuthorizationUrlResponse> authorizationUrl(
            @PathVariable String provider) {
        String url = authService.buildAuthorizationUrl(provider);
        return ApiResponse.ok(new OAuthAuthorizationUrlResponse(url));
    }

    @PostMapping("/oauth/{provider}/callback")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> callback(
            @PathVariable String provider, @Valid @RequestBody OAuthCallbackRequest request) {
        TokenResponse tokens = authService.login(provider, request.code(), request.state());
        return withRefreshCookie(tokens);
    }

    /**
     * refresh token은 더 이상 body가 아니라 {@link RefreshTokenCookieSupport#COOKIE_NAME} 쿠키로 받는다. 쿠키만으로
     * 동작해 preflight 없는 단순 요청(cross-site form 등)도 도달할 수 있으므로, Origin 헤더가 있는데 허용 목록에 없으면 거부한다.
     * refresh token 자체는 재발급(rotate)하지 않고 발급 시점의 만료 기간까지 그대로 재사용하므로, access token만 새로 내려주고 쿠키는 다시
     * 설정하지 않는다.
     */
    @PostMapping("/token/refresh")
    public ApiResponse<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshTokenCookieSupport.COOKIE_NAME, required = false)
                    String refreshToken,
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origin) {
        verifyOrigin(origin);
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        TokenResponse tokens = authService.refresh(refreshToken);
        return ApiResponse.ok(AccessTokenResponse.from(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser Long userId, HttpServletResponse response) {
        authService.logout(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieSupport.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@CurrentUser Long userId) {
        User user = authService.getCurrentUser(userId);
        return ApiResponse.ok(AuthUserResponse.from(user));
    }

    private ResponseEntity<ApiResponse<AccessTokenResponse>> withRefreshCookie(
            TokenResponse tokens) {
        ResponseCookie cookie = refreshTokenCookieSupport.issue(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(AccessTokenResponse.from(tokens)));
    }

    private void verifyOrigin(String origin) {
        if (StringUtils.hasText(origin) && !SecurityConfig.ALLOWED_ORIGINS.contains(origin)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }
}
