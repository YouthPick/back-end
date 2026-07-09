package com.bop.youthpick.global.config;

import com.bop.youthpick.auth.jwt.JwtAuthenticationFilter;
import com.bop.youthpick.auth.jwt.JwtTokenProvider;
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
 * SecurityContext를 채운다. 인증 실패는 {@link RestAuthenticationEntryPoint}가 JSON 401로 응답하도록 미리 연결해 둔다.
 *
 * <h2>현재 상태: 신규 도메인 API는 개발 편의상 permitAll</h2>
 *
 * 로그인 자체는 동작하지만, 다른 도메인(정책/게시판 등) 엔드포인트를 인증 필수로 바꾸는 작업은 이 이슈의 범위 밖이라 별도로 진행한다. 인증이 필요한 경로는 {@code
 * anyRequest().permitAll()} 앞에 {@code authenticated()} 규칙을 추가해 나간다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception.authenticationEntryPoint(restAuthenticationEntryPoint));

        http.authorizeHttpRequests(
                auth ->
                        auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()
                                .requestMatchers(
                                        "/api/v1/auth/oauth/**", "/api/v1/auth/token/refresh")
                                .permitAll()
                                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout")
                                .authenticated()
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
