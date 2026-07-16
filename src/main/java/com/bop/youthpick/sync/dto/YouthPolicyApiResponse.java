package com.bop.youthpick.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 온통청년 정책 API 응답 봉투 (docs/온통청년-API-명세.md). 성공 판정은 resultCode==200 && youthPolicyList 존재 —
 * PolicyApiClient(1-2)에서 수행.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthPolicyApiResponse(
        @JsonProperty("resultCode") Integer resultCode,
        @JsonProperty("resultMessage") String resultMessage,
        @JsonProperty("result") Result result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("pagging") Paging paging,
            @JsonProperty("youthPolicyList") List<YouthPolicyItem> youthPolicyList) {}

    /** API 오타 그대로 "pagging" — 우리 쪽 이름만 Paging으로 교정 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Paging(
            @JsonProperty("totCount") Integer totCount,
            @JsonProperty("pageNum") Integer pageNum,
            @JsonProperty("pageSize") Integer pageSize) {}
}
