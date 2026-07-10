package com.bop.youthpick.sync.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 온통청년 API 응답 픽스처(JSON) 역직렬화 검증. 명세서 표기(sBizCd)와 다른 실제 키(sbizCd), 빈 문자열, 버리는 필드(bscPlanCycl) 케이스
 * 포함.
 */
class YouthPolicyApiResponseTest {

    private static YouthPolicyApiResponse response;

    @BeforeAll
    static void deserializeFixture() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream json =
                YouthPolicyApiResponseTest.class.getResourceAsStream(
                        "/fixtures/youth-policy-response.json")) {
            response = objectMapper.readValue(json, YouthPolicyApiResponse.class);
        }
    }

    @Test
    @DisplayName("A: 응답 봉투(resultCode, pagging.totCount, 리스트 크기)를 역직렬화한다")
    void deserializesEnvelope() {
        assertThat(response.resultCode()).isEqualTo(200);
        assertThat(response.result().paging().totCount()).isEqualTo(2635);
        assertThat(response.result().youthPolicyList()).hasSize(2);
    }

    @Test
    @DisplayName("A: 정책 필드를 원문 그대로(변환 없이) 담는다")
    void deserializesPolicyFieldsAsRawStrings() {
        YouthPolicyItem item = response.result().youthPolicyList().get(0);

        assertThat(item.plcyNo()).isEqualTo("R2024033012016");
        assertThat(item.plcyNm()).isEqualTo("청년 월세 특별지원");
        assertThat(item.sprtTrgtMinAge()).isEqualTo("19");
        assertThat(item.earnMaxAmt()).isEqualTo("5000");
        assertThat(item.aplyYmd()).isEqualTo("20260101 ~ 20261231");
        assertThat(item.zipCd()).isEqualTo("11110,11140,11170");
        assertThat(item.sprtArvlSeqYn()).isEqualTo("Y");
        assertThat(item.lastMdfcnDt()).isEqualTo("2026-06-15 14:22:01");
    }

    @Test
    @DisplayName("X: 실제 JSON 키는 소문자 sbizCd — 오타 시 null이 되므로 값 존재를 확인한다")
    void mapsLowercaseSbizCdKey() {
        YouthPolicyItem item = response.result().youthPolicyList().get(0);

        assertThat(item.sbizCd()).isEqualTo("0014010");
    }

    @Test
    @DisplayName("X: 빈 문자열 필드는 빈 문자열 그대로, 변환은 Processor 책임")
    void keepsEmptyStringsUntouched() {
        YouthPolicyItem ongoing = response.result().youthPolicyList().get(1);

        assertThat(ongoing.aplyYmd()).isEmpty();
        assertThat(ongoing.sprtArvlSeqYn()).isEmpty();
        assertThat(ongoing.zipCd()).isEmpty();
        assertThat(ongoing.plcyExplnCn()).isEqualTo("   ");
    }

    @Test
    @DisplayName("E: 적재하지 않는 필드(bscPlanCycl 등)가 있어도 역직렬화에 실패하지 않는다")
    void ignoresUnknownFields() {
        assertThat(response.result().youthPolicyList().get(0).plcyNo()).isNotNull();
    }
}
