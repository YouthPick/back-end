package com.bop.youthpick.global.common;

import java.util.Map;

/**
 * 성공 응답 공통 봉투. 컨트롤러는 엔티티를 직접 반환하지 않고 항상 이 타입으로 감싼다.
 * 대부분의 경우 (기본 http 상태 코드가 200인 경우) 응답 Response로 사용하면 된다.
 * 상태 코드가 필요한 경우, ResponseEntity<ApiResponse<?>> ?에 타입을 넣고 맞춰서 사용
 * meta에는 page, totalCount 등 data가 아닌데 필요한 부가정보를 등록
 * 에러 응답은 이 타입이 아니라 {@code global.error.ErrorResponse} 형식으로 내려간다.
 */
public record ApiResponse<T>(
        T data,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, Map.of());
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(data, meta);
    }
}
