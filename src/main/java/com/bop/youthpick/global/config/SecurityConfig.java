package com.bop.youthpick.global.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 보안 기본 골격. REST API 기준으로 세션을 쓰지 않고(STATELESS), CSRF는 끄고, CORS만 켠다.
 * 인증 실패는 {@link RestAuthenticationEntryPoint}가 JSON 401로 응답하도록 미리 연결해 둔다.
 *
 * <h2>현재 상태: 개발용(모든 요청 허용)</h2>
 * 인증 기능(로그인)이 아직 없으므로, 팀원들이 각자 API를 개발·테스트할 수 있도록
 * {@code anyRequest().permitAll()}로 전부 열어 둔다. 보안 자체는 아래 절차로 언제든 켤 수 있다.
 *
 * <h2>인증 적용 절차 (팀원의 auth 코드가 준비되면)</h2>
 * <ol>
 *   <li>{@code com.bop.youthpick.auth} 패키지(JwtAuthenticationFilter 등)를 추가한다.
 *       JWT/Redis 관련 의존성(jjwt 등)과 설정(application.yml의 youthpick.auth.*)도 함께 채운다.</li>
 *   <li>아래 "AUTH ON" 주석 블록을 활성화하고, 현재 "DEV: 전부 허용" 블록을 제거한다.
 *       (JwtAuthenticationFilter 주입 필드 + addFilterBefore + 공개경로/authenticated)</li>
 * </ol>
 * 이렇게 두면 EntryPoint·CORS·STATELESS 골격은 그대로 재사용되고, 바뀌는 건 인가 규칙과 필터뿐이다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    // [AUTH ON] 1단계: 팀원 auth 코드 추가 후 아래 주입을 활성화한다.
    // private final com.bop.youthpick.auth.token.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(restAuthenticationEntryPoint));

        // ===== DEV: 전부 허용 (로그인 기능 생기기 전까지 개발 편의용) =====
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
        );

        // ===== [AUTH ON] 2단계: 위 DEV 블록을 지우고 아래를 활성화한다 =====
        // http.authorizeHttpRequests(auth -> auth
        //         .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        //         .requestMatchers(
        //                 "/actuator/health",
        //                 "/error",
        //                 // 인증 없이 접근 가능한 공개 엔드포인트 (팀원 auth/정책 API 기준)
        //                 "/api/v1/auth/oauth/*/authorization-url",
        //                 "/api/v1/auth/oauth/*/callback",
        //                 "/api/v1/auth/token/refresh",
        //                 "/api/v1/auth/logout",
        //                 "/api/v1/policies",
        //                 "/api/v1/policies/**"
        //         ).permitAll()
        //         .anyRequest().authenticated()
        // );
        // http.addFilterBefore(jwtAuthenticationFilter,
        //         org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 개발 서버 주소. 실제 배포 도메인에 맞게 조정한다.
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
