package com.bop.youthpick.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 부가정보. Map 대신 타입을 명시해 Swagger(OpenAPI) 문서에 필드 스키마가 그대로 드러나게 한다.
 *
 * <p>{@code page}는 1부터 시작한다(1-based). {@link Page#getNumber()}는 Spring Data 내부 규약대로 0-based라 여기서 +1
 * 해서 응답 규약을 요청 파라미터(`spring.data.web.pageable.one-indexed-parameters=true`)와 맞춘다.
 *
 * <p>필드에 {@code example}을 주지 않으면 Swagger UI가 int/long 기본 샘플 값(2^30, 2^53-1 등 의미 없는 큰 수)을 채워 넣으므로 실제
 * 값처럼 보이는 예시를 명시한다.
 */
public record PageMeta(
        @Schema(description = "현재 페이지 번호(1부터 시작)", example = "1") int page,
        @Schema(description = "전체 데이터 개수", example = "42") long totalCount,
        @Schema(description = "전체 페이지 수", example = "5") int totalPages) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber() + 1, page.getTotalElements(), page.getTotalPages());
    }
}
