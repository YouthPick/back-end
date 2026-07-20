package com.bop.youthpick.global.common;

import org.springframework.data.domain.Page;

/**
 * 페이지네이션 부가정보. Map 대신 타입을 명시해 Swagger(OpenAPI) 문서에 필드 스키마가 그대로 드러나게 한다.
 *
 * <p>{@code page}는 1부터 시작한다(1-based). {@link Page#getNumber()}는 Spring Data 내부 규약대로 0-based라 여기서 +1
 * 해서 응답 규약을 요청 파라미터(`spring.data.web.pageable.one-indexed-parameters=true`)와 맞춘다.
 */
public record PageMeta(int page, long totalCount, int totalPages) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber() + 1, page.getTotalElements(), page.getTotalPages());
    }
}
