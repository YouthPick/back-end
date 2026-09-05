package com.bop.youthpick.search.config;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 클라이언트가 쓰는 JSON 변환기.
 *
 * <p>기본 변환기는 {@code LocalDate}/{@code LocalDateTime}을 모른다("Java 8 date/time type not supported").
 * 웹 응답용 ObjectMapper 를 재사용하지 않고 따로 만드는 이유는, API 응답 포맷을 바꾸는 설정이 색인 문서 포맷까지 흔들면
 * 안 되기 때문이다 — 둘은 다른 계약이다.
 *
 * <p>Spring Boot 의 기본 {@code JsonpMapper} 는 {@code @ConditionalOnMissingBean} 이라 이 빈이 우선한다.
 */
@Configuration
public class ElasticsearchJsonConfig {

    @Bean
    public JsonpMapper elasticsearchJsonpMapper() {
        return new JacksonJsonpMapper(
                JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        // 날짜를 epoch 숫자가 아니라 "2026-09-06" 문자열로 보낸다.
                        // 매핑의 date 타입이 기본으로 이해하는 형식이고, 색인을 눈으로 확인하기도 쉽다.
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        // null 필드는 아예 보내지 않는다. 색인 크기가 줄고 "값 없음"이 명확해진다.
                        .serializationInclusion(JsonInclude.Include.NON_NULL)
                        .build());
    }
}
