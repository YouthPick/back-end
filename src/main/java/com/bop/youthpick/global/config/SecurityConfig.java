package com.bop.youthpick.global.config;

import com.bop.youthpick.auth.service.JwtAuthenticationFilter;
import com.bop.youthpick.auth.service.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 보안 기본 골격. REST API 기준으로 세션을 쓰지 않고(STATELESS) CSRF는 끄고, CORS만 켠다. 인증은 JWT access/refresh token
 * 기반이며 {@code JwtAuthenticationFilter}가 매 요청의 {@code Authorization: Bearer} 헤더를 해석해
 * SecurityContext를 채운다. 인증 실패는 {@link RestAuthenticationEntryPoint}가 JSON 401로, 권한 부족은 {@link
 * RestAccessDeniedHandler}가 JSON 403으로 응답하도록 미리 연결해 둔다.
 *
 * <h2>인가 규칙: API 명세서(docs)의 권한 컬럼 기준</h2>
 *
 * 관리자 전용은 {@code hasRole("ADMIN")}, 회원 전용은 {@code authenticated()}, 비회원/공통은 {@code permitAll()}로
 * 매핑한다. 아직 컨트롤러가 없는 경로도 명세에 있으면 미리 규칙을 걸어 둔다(나중에 컨트롤러가 추가돼도 기본값이 열려 있지 않도록). 명세에 없는 나머지 경로는 여전히 개발
 * 편의상 {@code anyRequest().permitAll()}로 열어 둔다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                                        .accessDeniedHandler(restAccessDeniedHandler));

        http.authorizeHttpRequests(
                auth ->
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()
                                // 관리자(ADMIN) 전용 — 정책 수집 실행/이력 조회
                                .requestMatchers("/api/v1/admin/**")
                                .hasRole("ADMIN")
                                // 비회원(공개) — 로그인 자체, 정책 탐색/비교/검색, 메타 조회, 헬스체크
                                .requestMatchers(
                                        "/api/v1/auth/oauth/**",
                                        "/api/v1/auth/token/refresh",
                                        "/api/v1/policy-chat/queries",
                                        "/api/v1/policy-comparisons/**",
                                        "/api/v1/meta/profile-options",
                                        "/api/v1/policies/**",
                                        "/api/v1/health")
                                .permitAll()
                                // 회원 전용 — 로그인 상태 조회/탈퇴/로그아웃, 마이페이지(관심정책/추천/읽음/프로필), 챗봇 프로필 동의
                                .requestMatchers(
                                        "/api/v1/auth/me",
                                        "/api/v1/auth/logout",
                                        "/api/v1/me/**",
                                        "/api/v1/policy-chat/profile-consent")
                                .authenticated()
                                // 명세에 없는 나머지 경로(구현 중인 다른 도메인 등)는 개발 편의상 열어 둔다.
                                .anyRequest()
                                .permitAll());

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 개발 서버 주소. 실제 배포 도메인에 맞게 조정한다.
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
