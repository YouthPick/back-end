package com.bop.youthpick.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void meta_없는_응답은_JSON에서_meta가_생략된다() throws Exception {
        String json = objectMapper.writeValueAsString(ApiResponse.ok("hello"));

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("data").asText()).isEqualTo("hello");
        assertThat(node.has("meta")).isFalse();
    }

    @Test
    void 페이지_응답은_meta에_page_totalCount_totalPages가_담긴다() throws Exception {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        String json = objectMapper.writeValueAsString(ApiResponse.ok(page.getContent(), page));

        JsonNode meta = objectMapper.readTree(json).get("meta");
        assertThat(meta.get("page").asInt()).isEqualTo(1);
        assertThat(meta.get("totalCount").asLong()).isEqualTo(5);
        assertThat(meta.get("totalPages").asInt()).isEqualTo(3);
    }
}
