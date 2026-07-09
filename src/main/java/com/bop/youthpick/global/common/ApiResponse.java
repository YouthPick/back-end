package com.bop.youthpick.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

/**
 * 성공 응답 공통 봉투. 컨트롤러는 엔티티를 직접 반환하지 않고 항상 이 타입으로 감싼다. 대부분의 경우 (기본 http 상태 코드가 200인 경우) 응답 Response로
 * 사용하면 된다. 상태 코드가 필요한 경우, ResponseEntity<ApiResponse<?>> ?에 타입을 넣고 맞춰서 사용. 페이지 목록 응답은 {@code ok(data, page)}로
 * 감싸면 meta에 page/totalCount/totalPages가 담긴다. meta가 없으면 응답 JSON에서 생략된다. 에러 응답은 이 타입이 아니라
 * {@code global.error.ErrorResponse} 형식으로 내려간다.
 */
public record ApiResponse<T>(T data, @JsonInclude(JsonInclude.Include.NON_NULL) PageMeta meta) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Page<?> page) {
        return new ApiResponse<>(data, PageMeta.from(page));
    }
}
