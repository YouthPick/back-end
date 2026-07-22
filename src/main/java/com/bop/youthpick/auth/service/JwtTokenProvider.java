package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** JWT access/refresh 토큰 발급·검증. refresh 토큰의 실제 유효성(폐기 여부)은 {@code RefreshTokenStore}가 관리한다. */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, TOKEN_TYPE_ACCESS, role, jwtProperties.accessTokenExpiration());
    }

    public String createRefreshToken(Long userId) {
        return createToken(
                userId, TOKEN_TYPE_REFRESH, null, jwtProperties.refreshTokenExpiration());
    }

    public long accessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration().toSeconds();
    }

    public Duration refreshTokenExpiration() {
        return jwtProperties.refreshTokenExpiration();
    }

    /** access token의 서명·만료·타입을 검증하고, 요청당 재파싱을 피하기 위해 {@link Claims}를 그대로 반환한다. */
    public Claims validateAccessToken(String token) {
        return requireClaims(token, TOKEN_TYPE_ACCESS);
    }

    /** refresh token의 서명·만료·타입을 검증하고 {@link Claims}를 반환한다. */
    public Claims validateRefreshToken(String token) {
        return requireClaims(token, TOKEN_TYPE_REFRESH);
    }

    public Long getUserId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    private Claims requireClaims(String token, String expectedType) {
        Claims claims = parseClaims(token);
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        return claims;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private String createToken(Long userId, String type, String role, Duration ttl) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttl.toMillis());

        JwtBuilder builder =
                Jwts.builder()
                        .subject(String.valueOf(userId))
                        .claim(CLAIM_TYPE, type)
                        .issuedAt(now)
                        .expiration(expiration)
                        .signWith(key);
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        return builder.compact();
    }
}
