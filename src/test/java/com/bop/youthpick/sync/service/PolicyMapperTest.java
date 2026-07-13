package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.sync.dto.YouthPolicyApiResponse;
import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PolicyMapper 순수 단위 테스트 — 자가진단 정확도에 직결되는 전처리 규칙 검증(배치 설계 §8 최우선). 정상 케이스는 실측 픽스처의 전 필드를 대조해
 * Policy.create 위치 파라미터 순서 실수까지 잡는다.
 */
class PolicyMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PolicyMapper mapper = new PolicyMapper(objectMapper);

    private YouthPolicyItem fixtureItem(int index) throws IOException {
        try (InputStream in =
                getClass().getResourceAsStream("/fixtures/youth-policy-response.json")) {
            return objectMapper
                    .readValue(in, YouthPolicyApiResponse.class)
                    .result()
                    .youthPolicyList()
                    .get(index);
        }
    }

    private YouthPolicyItem item(String json) throws IOException {
        return objectMapper.readValue(json, YouthPolicyItem.class);
    }

    @Test
    @DisplayName("A: 정상 정책은 45개 필드 전부가 규칙대로 매핑된다 (파라미터 순서 실수 감지)")
    void mapsAllFieldsFromNormalItem() throws IOException {
        Policy policy = mapper.toEntity(fixtureItem(0));

        assertThat(policy.getPolicyNo()).isEqualTo("R2024033012016");
        assertThat(policy.getTitle()).isEqualTo("청년 월세 특별지원");
        assertThat(policy.getDescription()).isEqualTo("경제적 어려움을 겪는 청년층의 주거비 부담 경감");
        assertThat(policy.getSupportContent()).isEqualTo("월 최대 20만원 월세 지원 (12개월)");
        assertThat(policy.getKeywords()).isEqualTo("월세,주거비,청년");
        assertThat(policy.getCategory()).isEqualTo("주거");
        assertThat(policy.getMiddleCategory()).isEqualTo("주택 및 거주지");
        assertThat(policy.getOrganizationName()).isEqualTo("국토교통부");
        assertThat(policy.getMinAge()).isEqualTo(19);
        assertThat(policy.getMaxAge()).isEqualTo(34);
        assertThat(policy.getJobCodes()).isEqualTo("0013001,0013002");
        assertThat(policy.getSchoolCodes()).isEqualTo("0049010");
        assertThat(policy.getIncomeConditionCode()).isEqualTo("0043002");
        assertThat(policy.getIncomeMaxAmount()).isEqualTo(5000);
        assertThat(policy.getIncomeEtcContent()).isNull(); // 빈 문자열 → null
        assertThat(policy.getMaritalStatusCode()).isEqualTo("0055003");
        assertThat(policy.getMajorCodes()).isEqualTo("0011009");
        assertThat(policy.getSpecializationCodes()).isEqualTo("0014010");
        assertThat(policy.getAdditionalQualification()).isEqualTo("무주택자");
        assertThat(policy.getParticipationRestriction()).isEqualTo("주택 소유자 제외");
        assertThat(policy.getApplicationPeriodType()).isEqualTo("0057001");
        assertThat(policy.getApplicationPeriodRaw()).isEqualTo("20260101 ~ 20261231");
        assertThat(policy.getApplicationStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(policy.getApplicationEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(policy.getBusinessPeriodBegin()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(policy.getBusinessPeriodEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(policy.getBusinessPeriodEtc()).isNull();
        assertThat(policy.getSupportScaleCount()).isEqualTo(15000);
        assertThat(policy.isFirstComeFirstServed()).isTrue();
        assertThat(policy.getApplicationUrl()).isEqualTo("https://www.myhome.go.kr");
        assertThat(policy.getReferenceUrl1()).isEqualTo("https://www.molit.go.kr");
        assertThat(policy.getReferenceUrl2()).isNull();
        assertThat(policy.getApplicationMethod()).isEqualTo("온라인 신청");
        assertThat(policy.getSubmissionDocuments()).isEqualTo("신분증, 임대차계약서");
        assertThat(policy.getScreeningMethod()).isEqualTo("서류 심사");
        assertThat(policy.getAgeLimitFlag()).isEqualTo("Y");
        assertThat(policy.getIncomeMinAmount()).isZero();
        assertThat(policy.getSupportScaleLimit()).isTrue();
        assertThat(policy.getOperatingInstitutionName()).isEqualTo("한국토지주택공사");
        assertThat(policy.getApprovalStatusCode()).isEqualTo("0044002");
        assertThat(policy.getEtcMatters()).isNull();
        assertThat(policy.getViewCount()).isEqualTo(125478);
        assertThat(policy.getFirstRegisteredAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 15, 30));
        assertThat(policy.getLastModifiedAt()).isEqualTo(LocalDateTime.of(2026, 6, 15, 14, 22, 1));
        assertThat(policy.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
        assertThat(policy.getRawPayload()).contains("\"plcyNo\":\"R2024033012016\"");
    }

    @Test
    @DisplayName("A: 상시(0057002)·빈값 정책은 날짜 스킵, 빈 Y/N은 false, 공백 설명은 null")
    void mapsAlwaysOpenItemWithBlanks() throws IOException {
        Policy policy = mapper.toEntity(fixtureItem(1));

        assertThat(policy.getApplicationStartDate()).isNull();
        assertThat(policy.getApplicationEndDate()).isNull();
        assertThat(policy.getApplicationPeriodRaw()).isNull(); // 빈 aplyYmd
        assertThat(policy.getDescription()).isNull(); // "   " 공백만 → null
        assertThat(policy.isFirstComeFirstServed()).isFalse(); // "" → false
        assertThat(policy.getSupportScaleLimit()).isFalse(); // "" → false
        assertThat(policy.getBusinessPeriodBegin()).isNull();
        assertThat(policy.getBusinessPeriodEnd()).isNull();
        assertThat(policy.getOrganizationName()).isEqualTo("중소벤처기업부");
        assertThat(policy.getOperatingInstitutionName()).isNull(); // "" → null
        assertThat(policy.getViewCount()).isEqualTo(302);
    }

    @Test
    @DisplayName("E: 깨진 숫자/날짜/일시는 예외 없이 해당 필드만 null (viewCount는 0)")
    void brokenValuesBecomeNullWithoutException() throws IOException {
        Policy policy =
                mapper.toEntity(
                        item(
                                """
                                {"plcyNo":"BAD1","plcyNm":"깨진 정책",
                                 "sprtTrgtMinAge":"열아홉","earnMaxAmt":"5,000","inqCnt":"많음",
                                 "bizPrdBgngYmd":"2026-01-01","frstRegDt":"2026/01/02 09:00:00",
                                 "aplyPrdSeCd":"0057001","aplyYmd":"20260101 ~ 2026123X"}
                                """));

        assertThat(policy.getMinAge()).isNull();
        assertThat(policy.getIncomeMaxAmount()).isNull();
        assertThat(policy.getViewCount()).isZero();
        assertThat(policy.getBusinessPeriodBegin()).isNull(); // yyyyMMdd 아님
        assertThat(policy.getFirstRegisteredAt()).isNull();
        assertThat(policy.getApplicationStartDate()).isEqualTo(LocalDate.of(2026, 1, 1)); // 앞쪽은 정상
        assertThat(policy.getApplicationEndDate()).isNull(); // 마감일만 깨짐
        assertThat(policy.getApplicationPeriodRaw()).isEqualTo("20260101 ~ 2026123X"); // 원문은 보존
    }

    @Test
    @DisplayName("X: aplyYmd에 구분자(~)가 없거나, 상시 코드면 날짜가 있어도 파싱하지 않는다")
    void applyPeriodEdgeCases() throws IOException {
        Policy noSeparator =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X1\",\"aplyPrdSeCd\":\"0057001\",\"aplyYmd\":\"20260101\"}"));
        assertThat(noSeparator.getApplicationStartDate()).isNull();
        assertThat(noSeparator.getApplicationEndDate()).isNull();

        Policy alwaysOpen =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X2\",\"aplyPrdSeCd\":\"0057002\","
                                        + "\"aplyYmd\":\"20260101 ~ 20261231\"}"));
        assertThat(alwaysOpen.getApplicationStartDate()).isNull();
        assertThat(alwaysOpen.getApplicationEndDate()).isNull();
        assertThat(alwaysOpen.getApplicationPeriodRaw()).isEqualTo("20260101 ~ 20261231"); // 원문은 보존
    }

    @Test
    @DisplayName("X: 주관기관이 비면 운영기관으로 fallback, 둘 다 비면 null / N은 false")
    void organizationFallbackAndBooleanN() throws IOException {
        Policy fallback =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X3\",\"sprvsnInstCdNm\":\"  \","
                                        + "\"operInstCdNm\":\"운영기관\",\"sprtArvlSeqYn\":\"N\"}"));
        assertThat(fallback.getOrganizationName()).isEqualTo("운영기관");
        assertThat(fallback.isFirstComeFirstServed()).isFalse(); // "N" → false

        Policy bothBlank =
                mapper.toEntity(
                        item("{\"plcyNo\":\"X4\",\"sprvsnInstCdNm\":\"\",\"operInstCdNm\":\"\"}"));
        assertThat(bothBlank.getOrganizationName()).isNull();
    }

    // ---- toRegions: zipCd → PolicyRegion 정규화 (plan 2-2) ----

    private Policy policy(String plcyNo) throws IOException {
        return mapper.toEntity(item("{\"plcyNo\":\"" + plcyNo + "\"}"));
    }

    private static Map<String, Region> regionMap(String... codes) {
        Map<String, Region> map = new HashMap<>();
        for (String code : codes) {
            map.put(code, Region.create(code, "시도" + code, "시군구" + code));
        }
        return map;
    }

    @Test
    @DisplayName("A: 단일 코드는 PolicyRegion 1건으로, 정책·지역이 연결된다")
    void mapsSingleRegionCode() throws IOException {
        Policy policy = policy("R1");

        List<PolicyRegion> regions = mapper.toRegions(policy, "11110", regionMap("11110"));

        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).getPolicy()).isSameAs(policy);
        assertThat(regions.get(0).getRegion().getCode()).isEqualTo("11110");
    }

    @Test
    @DisplayName("A: 콤마 다중 목록은 행 단위로 분해되고, 중복 코드·공백 토큰은 접힌다")
    void mapsMultipleCodesWithDedup() throws IOException {
        List<PolicyRegion> regions =
                mapper.toRegions(
                        policy("R2"),
                        "11110, 11140 ,11110,,11170",
                        regionMap("11110", "11140", "11170"));

        assertThat(regions)
                .extracting(r -> r.getRegion().getCode())
                .containsExactly("11110", "11140", "11170");
    }

    @Test
    @DisplayName("A: 전국 정책(256개 코드)은 전부 행으로 정규화된다")
    void mapsNationwideCodeList() throws IOException {
        List<String> codes =
                IntStream.range(0, 256).mapToObj(i -> String.valueOf(10000 + i * 10)).toList();
        Map<String, Region> regions = regionMap(codes.toArray(String[]::new));

        List<PolicyRegion> result =
                mapper.toRegions(policy("R3"), String.join(",", codes), regions);

        assertThat(result).hasSize(256);
        assertThat(result.stream().map(r -> r.getRegion().getCode()).collect(Collectors.toSet()))
                .hasSize(256);
    }

    @Test
    @DisplayName("X: regions에 없는 코드는 FK 위반 대신 스킵하고 아는 코드만 매핑한다")
    void skipsUnknownCodes() throws IOException {
        List<PolicyRegion> regions =
                mapper.toRegions(policy("R4"), "11110,99999,11140", regionMap("11110", "11140"));

        assertThat(regions)
                .extracting(r -> r.getRegion().getCode())
                .containsExactly("11110", "11140");
    }

    @Test
    @DisplayName("X: zipCd가 빈값/공백/null이면 빈 리스트 (전국 아님 — 지역정보 없음)")
    void emptyZipCdMeansNoRegions() throws IOException {
        Policy policy = policy("R5");
        Map<String, Region> regions = regionMap("11110");

        assertThat(mapper.toRegions(policy, "", regions)).isEmpty();
        assertThat(mapper.toRegions(policy, "   ", regions)).isEmpty();
        assertThat(mapper.toRegions(policy, null, regions)).isEmpty();
    }
}
