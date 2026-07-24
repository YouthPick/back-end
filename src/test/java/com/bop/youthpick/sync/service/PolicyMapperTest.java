package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.policy.entity.Policy;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
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
    @DisplayName("대분류 정규화: 반각점·콤마다중·구명칭은 표준 5분류로, 미지의 값은 원본 유지, 빈값은 null (#80)")
    void normalizesCategoryVariants() {
        assertThat(mapper.normalizeCategory("P1", "일자리")).isEqualTo("일자리");
        assertThat(mapper.normalizeCategory("P1", "교육･직업훈련")).isEqualTo("교육·직업훈련");
        assertThat(mapper.normalizeCategory("P1", "금융･복지･문화,금융･복지･문화")).isEqualTo("금융·복지·문화");
        assertThat(mapper.normalizeCategory("P1", "참여권리")).isEqualTo("참여·기반");
        assertThat(mapper.normalizeCategory("P1", "신규분류")).isEqualTo("신규분류");
        assertThat(mapper.normalizeCategory("P1", "  ")).isNull();
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
    @DisplayName("X: aplyYmd가 비어있고 상시(0057002)가 아니면 사업기간으로 신청기간을 대체한다 (#123)")
    void fallsBackToBusinessPeriodWhenApplyPeriodMissing() throws IOException {
        Policy fallback =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X5\",\"aplyPrdSeCd\":\"0057003\","
                                        + "\"bizPrdBgngYmd\":\"20260301\",\"bizPrdEndYmd\":\"20260630\"}"));
        assertThat(fallback.getApplicationStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(fallback.getApplicationEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));

        // 상시(0057002)는 사업기간이 있어도 대체하지 않는다 — 원본이 명시한 "상시" 의미를 그대로 존중.
        Policy alwaysOpenWithBusinessPeriod =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X6\",\"aplyPrdSeCd\":\"0057002\","
                                        + "\"bizPrdBgngYmd\":\"20260301\",\"bizPrdEndYmd\":\"20260630\"}"));
        assertThat(alwaysOpenWithBusinessPeriod.getApplicationStartDate()).isNull();
        assertThat(alwaysOpenWithBusinessPeriod.getApplicationEndDate()).isNull();

        // 사업기간마저 없으면 그대로 null (판단 근거가 없는 경우)
        Policy noSignalAtAll =
                mapper.toEntity(item("{\"plcyNo\":\"X7\",\"aplyPrdSeCd\":\"0057003\"}"));
        assertThat(noSignalAtAll.getApplicationStartDate()).isNull();
        assertThat(noSignalAtAll.getApplicationEndDate()).isNull();
    }

    @Test
    @DisplayName("X: 신청·참고 URL은 스킴 없는 도메인에 https://를 붙이고, URL이 아닌 자유 텍스트는 null로 버린다 (#123)")
    void normalizesUrls() throws IOException {
        Policy schemeAdded =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X8\",\"aplyUrlAddr\":\"www.bokjiro.go.kr\","
                                        + "\"refUrlAddr1\":\"www.khug.or.kr/jeonse/index.js\"}"));
        assertThat(schemeAdded.getApplicationUrl()).isEqualTo("https://www.bokjiro.go.kr");
        assertThat(schemeAdded.getReferenceUrl1())
                .isEqualTo("https://www.khug.or.kr/jeonse/index.js");

        Policy notAUrl =
                mapper.toEntity(
                        item(
                                "{\"plcyNo\":\"X9\",\"aplyUrlAddr\":\"전화문의\","
                                        + "\"refUrlAddr1\":\"-\"}"));
        assertThat(notAUrl.getApplicationUrl()).isNull();
        assertThat(notAUrl.getReferenceUrl1()).isNull();

        Policy alreadyPrefixed =
                mapper.toEntity(
                        item("{\"plcyNo\":\"X10\",\"aplyUrlAddr\":\"https://www.molit.go.kr\"}"));
        assertThat(alreadyPrefixed.getApplicationUrl()).isEqualTo("https://www.molit.go.kr");
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

    // ---- resolveRegions: zipCd → Region 해석 (plan 2-2, 링크 생성은 Writer 몫) ----

    private static Map<String, Region> regionMap(String... codes) {
        Map<String, Region> map = new HashMap<>();
        for (String code : codes) {
            map.put(code, Region.create(code, "시도" + code, "시군구" + code));
        }
        return map;
    }

    @Test
    @DisplayName("A: 단일 코드는 Region 1건으로 해석된다")
    void mapsSingleRegionCode() {
        List<Region> regions = mapper.resolveRegions("R1", "11110", regionMap("11110"));

        assertThat(regions).hasSize(1);
        assertThat(regions.get(0).getCode()).isEqualTo("11110");
    }

    @Test
    @DisplayName("A: 콤마 다중 목록은 행 단위로 분해되고, 중복 코드·공백 토큰은 접힌다")
    void mapsMultipleCodesWithDedup() {
        List<Region> regions =
                mapper.resolveRegions(
                        "R2", "11110, 11140 ,11110,,11170", regionMap("11110", "11140", "11170"));

        assertThat(regions).extracting(Region::getCode).containsExactly("11110", "11140", "11170");
    }

    @Test
    @DisplayName("A: 전국 정책(256개 코드)은 전부 행으로 정규화된다")
    void mapsNationwideCodeList() {
        List<String> codes =
                IntStream.range(0, 256).mapToObj(i -> String.valueOf(10000 + i * 10)).toList();
        Map<String, Region> regions = regionMap(codes.toArray(String[]::new));

        List<Region> result = mapper.resolveRegions("R3", String.join(",", codes), regions);

        assertThat(result).hasSize(256);
        assertThat(result.stream().map(Region::getCode).collect(Collectors.toSet())).hasSize(256);
    }

    @Test
    @DisplayName("X: regions에 없는 코드는 FK 위반 대신 스킵하고 아는 코드만 매핑한다")
    void skipsUnknownCodes() {
        List<Region> regions =
                mapper.resolveRegions("R4", "11110,99999,11140", regionMap("11110", "11140"));

        assertThat(regions).extracting(Region::getCode).containsExactly("11110", "11140");
    }

    @Test
    @DisplayName("X: zipCd가 빈값/공백/null이면 빈 리스트 (전국 아님 — 지역정보 없음)")
    void emptyZipCdMeansNoRegions() {
        Map<String, Region> regions = regionMap("11110");

        assertThat(mapper.resolveRegions("R5", "", regions)).isEmpty();
        assertThat(mapper.resolveRegions("R5", "   ", regions)).isEmpty();
        assertThat(mapper.resolveRegions("R5", null, regions)).isEmpty();
    }

    // ---- 컬럼 길이 상한 절단 (#208) — Data truncation으로 정책 1건이 통째로 버려지는 문제 ----

    @Test
    @DisplayName("E: 컬럼 길이를 넘는 필드는 예외 없이 상한까지 잘려서 저장된다 (#208)")
    void truncatesFieldsThatExceedColumnLength() throws IOException {
        String longPolicyNo = "N".repeat(PolicyMapper.POLICY_NO_MAX_LENGTH + 5);
        String longTitle = "가".repeat(PolicyMapper.TITLE_MAX_LENGTH + 50);
        String longDescription = "나".repeat(PolicyMapper.DESCRIPTION_MAX_LENGTH + 10);
        String longUrl =
                "https://example.com/" + "a".repeat(PolicyMapper.APPLICATION_URL_MAX_LENGTH);

        Policy policy =
                mapper.toEntity(
                        item(
                                objectMapper.writeValueAsString(
                                        Map.of(
                                                "plcyNo", longPolicyNo,
                                                "plcyNm", longTitle,
                                                "plcyExplnCn", longDescription,
                                                "aplyUrlAddr", longUrl))));

        assertThat(policy.getPolicyNo())
                .hasSize(PolicyMapper.POLICY_NO_MAX_LENGTH)
                .isEqualTo(longPolicyNo.substring(0, PolicyMapper.POLICY_NO_MAX_LENGTH));
        assertThat(policy.getTitle())
                .hasSize(PolicyMapper.TITLE_MAX_LENGTH)
                .isEqualTo(longTitle.substring(0, PolicyMapper.TITLE_MAX_LENGTH));
        assertThat(policy.getDescription())
                .hasSize(PolicyMapper.DESCRIPTION_MAX_LENGTH)
                .isEqualTo(longDescription.substring(0, PolicyMapper.DESCRIPTION_MAX_LENGTH));
        assertThat(policy.getApplicationUrl())
                .hasSize(PolicyMapper.APPLICATION_URL_MAX_LENGTH)
                .isEqualTo(longUrl.substring(0, PolicyMapper.APPLICATION_URL_MAX_LENGTH));
    }

    @Test
    @DisplayName("X: 상한과 정확히 같은 길이는 잘리지 않는다 (경계)")
    void doesNotTruncateWhenExactlyAtLimit() throws IOException {
        String exactTitle = "다".repeat(PolicyMapper.TITLE_MAX_LENGTH);

        Policy policy =
                mapper.toEntity(
                        item(
                                objectMapper.writeValueAsString(
                                        Map.of("plcyNo", "EXACT1", "plcyNm", exactTitle))));

        assertThat(policy.getTitle()).hasSize(PolicyMapper.TITLE_MAX_LENGTH).isEqualTo(exactTitle);
    }

    @Test
    @DisplayName("E: 길이 초과 절단은 경고 로그에 정책번호·필드명·실제 길이·상한을 남긴다 (#208)")
    void logsWarningWithPolicyNoFieldLengthAndLimitOnTruncation() throws IOException {
        String longTitle = "라".repeat(PolicyMapper.TITLE_MAX_LENGTH + 7);
        CapturingAppender appender = CapturingAppender.attachTo(PolicyMapper.class);
        try {
            mapper.toEntity(
                    item(
                            objectMapper.writeValueAsString(
                                    Map.of("plcyNo", "WARN1", "plcyNm", longTitle))));

            assertThat(appender.warnMessages())
                    .anySatisfy(
                            message -> {
                                assertThat(message).contains("WARN1");
                                assertThat(message).contains("plcyNm");
                                assertThat(message).contains(String.valueOf(longTitle.length()));
                                assertThat(message)
                                        .contains(String.valueOf(PolicyMapper.TITLE_MAX_LENGTH));
                            });
        } finally {
            appender.detach();
        }
    }

    @Test
    @DisplayName("X: 길이 상한 내 정상 값은 절단 경고 로그를 남기지 않는다")
    void doesNotLogWarningWhenWithinLimit() throws IOException {
        CapturingAppender appender = CapturingAppender.attachTo(PolicyMapper.class);
        try {
            mapper.toEntity(fixtureItem(0));

            assertThat(appender.warnMessages()).noneMatch(message -> message.contains("길이 초과"));
        } finally {
            appender.detach();
        }
    }

    /** PolicyMapper가 SLF4J로 남기는 WARN 로그를 잡아내는 테스트 전용 Log4j2 Appender. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final org.apache.logging.log4j.core.Logger targetLogger;

        private CapturingAppender(org.apache.logging.log4j.core.Logger targetLogger) {
            super("PolicyMapperTest-capturing-appender", null, null, false, Property.EMPTY_ARRAY);
            this.targetLogger = targetLogger;
        }

        static CapturingAppender attachTo(Class<?> loggerClass) {
            org.apache.logging.log4j.core.Logger logger =
                    (org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerClass);
            CapturingAppender appender = new CapturingAppender(logger);
            appender.start();
            logger.addAppender(appender);
            return appender;
        }

        void detach() {
            targetLogger.removeAppender(this);
            stop();
        }

        List<String> warnMessages() {
            return messages;
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel().equals(Level.WARN)) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        }
    }
}
