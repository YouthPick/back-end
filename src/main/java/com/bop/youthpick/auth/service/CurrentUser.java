package com.bop.youthpick.auth.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Long} 파라미터에 붙이면 {@code Authorization} 헤더의 JWT access token(= {@code
 * JwtAuthenticationFilter}가 채운 인증 principal)에서 userId를 읽어 그대로 주입한다. DB 조회는 하지 않는다. 인증되지 않은 요청이면
 * {@code AuthErrorCode.UNAUTHORIZED}로 거부된다.
 *
 * <p>비회원도 허용하는 공개 엔드포인트에서 로그인 사용자만 선택적으로 식별하려면 {@code required = false}로 지정한다. 이 경우 인증되지 않은 요청에는 거부
 * 대신 {@code null}이 주입된다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    boolean required() default true;
}
