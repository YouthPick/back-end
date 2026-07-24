package com.bop.youthpick.sync.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 온통청년 raw DTO → Policy 엔티티 전처리 [Processor] (배치 설계 §4-1, 서비스 설계 §5-2).
 *
 * <p>전처리 규칙: 공백 문자열 → null trim, 숫자/날짜 문자열 → 타입 변환, "Y"/"N"/"" → Boolean(빈값 false),
 * aplyYmd("yyyyMMdd ~ yyyyMMdd") → 신청 시작/마감일 (상시 0057002·빈값은 파싱 스킵).
 *
 * <p>파싱 실패는 예외로 안 터뜨린다 — 해당 필드만 null + 경고 로그(plcyNo, 필드, 원문). 필드 하나 깨졌다고 정책 1건 전체(나머지 40여 필드)를 버리는
 * 게 더 큰 손실이기 때문(설계 §5 실패 격리: 전처리 실패 단위 = 필드).
 *
 * <p>컬럼 길이 초과도 같은 원칙이다 — 예외 대신 절단 + 경고 로그(plcyNo, 필드, 실제 길이, 상한)로 처리한다(#208). 길이 상한 상수는 {@link
 * com.bop.youthpick.policy.entity.Policy}의 {@code @Column(length = ...)}와 반드시 일치해야 하며,
 * PolicyMapperLengthLimitsTest가 리플렉션으로 드리프트를 검증한다.
 */
@Component
@RequiredArgsConstructor
public class PolicyMapper {

    private static final Logger log = LoggerFactory.getLogger(PolicyMapper.class);

    /** aplyPrdSeCd 상시 코드 — 신청기간 파싱 대상 아님 */
    private static final String ALWAYS_OPEN_CODE = "0057002";

    /**
     * 컬럼 길이 상한 — {@link com.bop.youthpick.policy.entity.Policy}의 {@code @Column(length = ...)}와 반드시
     * 같은 값이어야 한다(PolicyMapperLengthLimitsTest가 드리프트를 검증). TEXT/LONGTEXT 컬럼(길이 제한 없음)은 대상 아님.
     */
    static final int POLICY_NO_MAX_LENGTH = 32;

    static final int TITLE_MAX_LENGTH = 300;
    static final int DESCRIPTION_MAX_LENGTH = 2000;
    static final int KEYWORDS_MAX_LENGTH = 500;
    static final int CATEGORY_MAX_LENGTH = 64;
    static final int MIDDLE_CATEGORY_MAX_LENGTH = 64;
    static final int ORGANIZATION_NAME_MAX_LENGTH = 255;
    static final int JOB_CODES_MAX_LENGTH = 255;
    static final int SCHOOL_CODES_MAX_LENGTH = 255;
    static final int INCOME_CONDITION_CODE_MAX_LENGTH = 16;
    static final int MARITAL_STATUS_CODE_MAX_LENGTH = 16;
    static final int MAJOR_CODES_MAX_LENGTH = 255;
    static final int SPECIALIZATION_CODES_MAX_LENGTH = 255;
    static final int APPLICATION_PERIOD_TYPE_MAX_LENGTH = 16;
    static final int APPLICATION_PERIOD_RAW_MAX_LENGTH = 255;
    static final int APPLICATION_URL_MAX_LENGTH = 1000;
    static final int REFERENCE_URL_MAX_LENGTH = 1000;
    static final int AGE_LIMIT_FLAG_MAX_LENGTH = 4;
    static final int OPERATING_INSTITUTION_NAME_MAX_LENGTH = 255;
    static final int APPROVAL_STATUS_CODE_MAX_LENGTH = 16;

    /**
     * 대분류(lclsfNm) 표준 5분류 매핑 — 키는 가운뎃점(·・･) 제거 기준 (#80). 원본에는 반각점(U+FF65) 구분자, 콤마 다중값('일자리,일자리'),
     * 구명칭('참여권리')이 섞여 있다. 기존 데이터는 V8 마이그레이션이 같은 규칙으로 정리한다.
     */
    private static final Map<String, String> CATEGORY_BY_STRIPPED =
            Map.ofEntries(
                    Map.entry("일자리", "일자리"),
                    Map.entry("주거", "주거"),
                    Map.entry("교육", "교육·직업훈련"),
                    Map.entry("교육직업훈련", "교육·직업훈련"),
                    Map.entry("복지문화", "금융·복지·문화"),
                    Map.entry("금융복지문화", "금융·복지·문화"),
                    Map.entry("참여권리", "참여·기반"),
                    Map.entry("참여기반", "참여·기반"));

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    /** 신규/변경 정책 1건 전처리. Policy.create 파라미터 순서 = Policy 필드 선언 순서. */
    public Policy toEntity(YouthPolicyItem item) {
        String rawPlcyNo = trimToNull(item.plcyNo());
        String plcyNo = truncate(rawPlcyNo, "plcyNo", rawPlcyNo, POLICY_NO_MAX_LENGTH);
        LocalDate businessBegin = toDate(plcyNo, "bizPrdBgngYmd", item.bizPrdBgngYmd());
        LocalDate businessEnd = toDate(plcyNo, "bizPrdEndYmd", item.bizPrdEndYmd());
        ApplyPeriod applyPeriod =
                parseApplyPeriod(
                        plcyNo, item.aplyPrdSeCd(), item.aplyYmd(), businessBegin, businessEnd);
        return Policy.create(
                plcyNo,
                truncate(plcyNo, "plcyNm", trimToNull(item.plcyNm()), TITLE_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "plcyExplnCn",
                        trimToNull(item.plcyExplnCn()),
                        DESCRIPTION_MAX_LENGTH),
                trimToNull(item.plcySprtCn()),
                truncate(plcyNo, "plcyKywdNm", trimToNull(item.plcyKywdNm()), KEYWORDS_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "lclsfNm",
                        normalizeCategory(plcyNo, item.lclsfNm()),
                        CATEGORY_MAX_LENGTH),
                truncate(plcyNo, "mclsfNm", trimToNull(item.mclsfNm()), MIDDLE_CATEGORY_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "sprvsnInstCdNm/operInstCdNm",
                        organizationName(item),
                        ORGANIZATION_NAME_MAX_LENGTH),
                toInt(plcyNo, "sprtTrgtMinAge", item.sprtTrgtMinAge()),
                toInt(plcyNo, "sprtTrgtMaxAge", item.sprtTrgtMaxAge()),
                truncate(plcyNo, "jobCd", trimToNull(item.jobCd()), JOB_CODES_MAX_LENGTH),
                truncate(plcyNo, "schoolCd", trimToNull(item.schoolCd()), SCHOOL_CODES_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "earnCndSeCd",
                        trimToNull(item.earnCndSeCd()),
                        INCOME_CONDITION_CODE_MAX_LENGTH),
                toInt(plcyNo, "earnMaxAmt", item.earnMaxAmt()),
                trimToNull(item.earnEtcCn()),
                truncate(
                        plcyNo,
                        "mrgSttsCd",
                        trimToNull(item.mrgSttsCd()),
                        MARITAL_STATUS_CODE_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "plcyMajorCd",
                        trimToNull(item.plcyMajorCd()),
                        MAJOR_CODES_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "sbizCd",
                        trimToNull(item.sbizCd()),
                        SPECIALIZATION_CODES_MAX_LENGTH),
                trimToNull(item.addAplyQlfcCndCn()),
                trimToNull(item.ptcpPrpTrgtCn()),
                truncate(
                        plcyNo,
                        "aplyPrdSeCd",
                        trimToNull(item.aplyPrdSeCd()),
                        APPLICATION_PERIOD_TYPE_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "aplyYmd",
                        trimToNull(item.aplyYmd()),
                        APPLICATION_PERIOD_RAW_MAX_LENGTH),
                applyPeriod.start(),
                applyPeriod.end(),
                businessBegin,
                businessEnd,
                trimToNull(item.bizPrdEtcCn()),
                toInt(plcyNo, "sprtSclCnt", item.sprtSclCnt()),
                toBoolean(item.sprtArvlSeqYn()),
                truncate(
                        plcyNo,
                        "aplyUrlAddr",
                        normalizeUrl(plcyNo, "aplyUrlAddr", item.aplyUrlAddr()),
                        APPLICATION_URL_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "refUrlAddr1",
                        normalizeUrl(plcyNo, "refUrlAddr1", item.refUrlAddr1()),
                        REFERENCE_URL_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "refUrlAddr2",
                        normalizeUrl(plcyNo, "refUrlAddr2", item.refUrlAddr2()),
                        REFERENCE_URL_MAX_LENGTH),
                trimToNull(item.plcyAplyMthdCn()),
                trimToNull(item.sbmsnDcmntCn()),
                trimToNull(item.srngMthdCn()),
                truncate(
                        plcyNo,
                        "sprtTrgtAgeLmtYn",
                        trimToNull(item.sprtTrgtAgeLmtYn()),
                        AGE_LIMIT_FLAG_MAX_LENGTH),
                toInt(plcyNo, "earnMinAmt", item.earnMinAmt()),
                toBoolean(item.sprtSclLmtYn()),
                truncate(
                        plcyNo,
                        "operInstCdNm",
                        trimToNull(item.operInstCdNm()),
                        OPERATING_INSTITUTION_NAME_MAX_LENGTH),
                truncate(
                        plcyNo,
                        "plcyAprvSttsCd",
                        trimToNull(item.plcyAprvSttsCd()),
                        APPROVAL_STATUS_CODE_MAX_LENGTH),
                trimToNull(item.etcMttrCn()),
                toViewCount(plcyNo, item.inqCnt()),
                toDateTime(plcyNo, "frstRegDt", item.frstRegDt()),
                toDateTime(plcyNo, "lastMdfcnDt", item.lastMdfcnDt()),
                toRawPayload(plcyNo, item));
    }

    /**
     * zipCd 콤마 목록 → Region 해석 (전국이면 256개 코드). 중복 코드는 1건으로 접고, regions에 없는 코드는 FK 위반을 막기 위해 경고 로그 +
     * 스킵한다 — zipCd는 현행 법정시군구코드 5자리(예: 전남광주통합특별시=12xxx)라 행정구역 개편으로 코드가 신설/폐지되면 시드에 없는 코드가 나타날 수 있다.
     *
     * <p>PolicyRegion 링크 생성은 Writer의 몫이다 — 변경 UPDATE 경로에서는 DB의 관리 엔티티에 링크를 걸어야 하므로 여기서 만들면 못 쓴다.
     *
     * @param regionsByCode 호출자(배치 Job)가 회차당 1회 로드한 지역 마스터 (code → Region)
     */
    public List<Region> resolveRegions(
            String policyNo, String zipCd, Map<String, Region> regionsByCode) {
        String raw = trimToNull(zipCd);
        if (raw == null) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<Region> regions = new ArrayList<>();
        for (String token : raw.split(",")) {
            String code = trimToNull(token);
            if (code == null || !seen.add(code)) {
                continue;
            }
            Region region = regionsByCode.get(code);
            if (region == null) {
                log.warn("정책 {} 알 수 없는 지역코드 '{}' — 스킵 (regions 시드 갱신 필요)", policyNo, code);
                continue;
            }
            regions.add(region);
        }
        return regions;
    }

    /** 대분류 정규화: 콤마 다중값은 첫 값, 가운뎃점 변형 통일 후 표준 5분류로. 표준 외 값은 경고 + 원본 유지(조용히 사라지는 것 방지). */
    String normalizeCategory(String plcyNo, String lclsfNm) {
        String raw = trimToNull(lclsfNm);
        if (raw == null) {
            return null;
        }
        String first = raw.split(",")[0].trim();
        String canonical = CATEGORY_BY_STRIPPED.get(first.replaceAll("[·・･]", ""));
        if (canonical == null) {
            log.warn("정책 {} 미지의 대분류 '{}' — 원본 유지 (분류 매핑표 갱신 필요)", plcyNo, first);
            return first;
        }
        return canonical;
    }

    private record ApplyPeriod(LocalDate start, LocalDate end) {}

    /**
     * "yyyyMMdd ~ yyyyMMdd" 파싱. 상시(0057002)는 스킵(경고 없음). 그 외 코드에서 aplyYmd가 비어 있거나 형식이 이상하면
     * 사업기간(bizPrdBgngYmd~bizPrdEndYmd)으로 대체한다 — 신청기간 자체를 안 주는 정책(주로 0057003 지역 단발성 모집)이 실제로는 이미
     * 끝났는데도 신청마감일 없음("상시")으로 계속 노출되는 문제를 막기 위함(#123). 형식 이상은 경고 로그를 남긴다.
     */
    private ApplyPeriod parseApplyPeriod(
            String plcyNo,
            String aplyPrdSeCd,
            String aplyYmd,
            LocalDate businessBegin,
            LocalDate businessEnd) {
        if (ALWAYS_OPEN_CODE.equals(trimToNull(aplyPrdSeCd))) {
            return new ApplyPeriod(null, null);
        }
        String raw = trimToNull(aplyYmd);
        if (raw == null) {
            return new ApplyPeriod(businessBegin, businessEnd);
        }
        String[] parts = raw.split("~");
        if (parts.length != 2) {
            log.warn("정책 {} 필드 aplyYmd 형식 이상 — 원문 '{}'", plcyNo, raw);
            return new ApplyPeriod(businessBegin, businessEnd);
        }
        return new ApplyPeriod(
                toDate(plcyNo, "aplyYmd(시작일)", parts[0]), toDate(plcyNo, "aplyYmd(마감일)", parts[1]));
    }

    /**
     * 신청·참고 URL 정규화. 이미 http(s)://면 그대로, "www.xxx.go.kr"처럼 스킴만 빠진 도메인이면 https://를 붙인다. "-",
     * "전화문의"처럼 URL이 아닌 자유 텍스트는 깨진 링크를 만들지 않도록 null로 버린다(#123).
     */
    private static String normalizeUrl(String plcyNo, String field, String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return v;
        }
        if (v.contains(" ") || v.contains("\t") || !v.contains(".")) {
            log.warn("정책 {} 필드 {} URL 형식 아님(자유 텍스트로 판단) — 원문 '{}'", plcyNo, field, v);
            return null;
        }
        return "https://" + v;
    }

    /** sprvsnInstCdNm(주관기관) 우선, 비어 있으면 operInstCdNm(운영기관) fallback */
    private String organizationName(YouthPolicyItem item) {
        String supervising = trimToNull(item.sprvsnInstCdNm());
        return supervising != null ? supervising : trimToNull(item.operInstCdNm());
    }

    /** 원문 보존 — 스키마 진화 시 백필용. DTO 재직렬화라 DTO에 없는 필드(버리는 17개)는 미보존. */
    private String toRawPayload(String plcyNo, YouthPolicyItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            log.warn("정책 {} raw payload 직렬화 실패: {}", plcyNo, e.getMessage());
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 컬럼 길이 상한 절단(#208). 초과분은 예외 대신 경고 로그(plcyNo, 필드, 실제 길이, 상한) 후 잘라서 저장한다 — 필드 하나의 길이 초과 때문에 정책
     * 1건(나머지 필드)을 통째로 버리지 않기 위함(클래스 javadoc 실패 격리 원칙과 동일).
     */
    private static String truncate(String plcyNo, String field, String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        log.warn(
                "정책 {} 필드 {} 길이 초과 — 실제 {}자 > 상한 {}자, 잘라서 저장",
                plcyNo,
                field,
                value.length(),
                maxLength);
        return value.substring(0, maxLength);
    }

    private static Integer toInt(String plcyNo, String field, String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            log.warn("정책 {} 필드 {} 숫자 파싱 실패 — 원문 '{}'", plcyNo, field, value);
            return null;
        }
    }

    /** viewCount는 엔티티가 NOT NULL(int) — 파싱 실패/빈값은 0 */
    private static int toViewCount(String plcyNo, String value) {
        Integer parsed = toInt(plcyNo, "inqCnt", value);
        return parsed == null ? 0 : parsed;
    }

    private static boolean toBoolean(String value) {
        return "Y".equals(trimToNull(value));
    }

    private static LocalDate toDate(String plcyNo, String field, String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v, YMD);
        } catch (DateTimeParseException e) {
            log.warn("정책 {} 필드 {} 날짜 파싱 실패 — 원문 '{}'", plcyNo, field, value);
            return null;
        }
    }

    private static LocalDateTime toDateTime(String plcyNo, String field, String value) {
        String v = trimToNull(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(v, DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("정책 {} 필드 {} 일시 파싱 실패 — 원문 '{}'", plcyNo, field, value);
            return null;
        }
    }
}
