package com.bop.youthpick.global.common;

import org.springframework.data.domain.Page;

/**
 * 페이지네이션 부가정보. Map 대신 타입을 명시해 Swagger(OpenAPI) 문서에 필드 스키마가 그대로 드러나게 한다.
 */
public record PageMeta(int page, long totalCount, int totalPages) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getTotalElements(), page.getTotalPages());
    }
}
