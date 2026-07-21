package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 청년정책 (온통청년 API 수집·전처리 결과). 각 필드 주석의 코드는 API 원본 키(plcyNo 등) — 매핑표 역할. [보류] 필드는 원천 데이터 품질 문제로 판정 로직
 * 사용 금지, 적재만 한다.
 */
@Entity
@Table(
        name = "policies",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_policies_policy_no", columnNames = "policy_no"),
        indexes = {
            @Index(name = "idx_policies_application_end", columnList = "application_end_date"),
            @Index(name = "idx_policies_view_count", columnList = "view_count"),
            @Index(name = "idx_policies_category", columnList = "category")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** plcyNo — 온통청년 식별자 (upsert 기준) */
    @Column(name = "policy_no", length = 32, nullable = false)
    private String policyNo;

    // ---- 표시/검색 ----

    /** plcyNm */
    @Column(length = 300, nullable = false)
    private String title;

    /** plcyExplnCn */
    @Column(length = 2000)
    private String description;

    /** plcySprtCn */
    @Column(name = "support_content", columnDefinition = "TEXT")
    private String supportContent;

    /** plcyKywdNm — 콤마목록 */
    @Column(length = 500)
    private String keywords;

    /** lclsfNm — 대분류 */
    @Column(length = 64)
    private String category;

    /** mclsfNm — 중분류 */
    @Column(name = "middle_category", length = 64)
    private String middleCategory;

    /** sprvsnInstCdNm 우선, operInstCdNm fallback */
    @Column(name = "organization_name", length = 255)
    private String organizationName;

    // ---- 자격: 자동판정용 ----

    /** sprtTrgtMinAge — 0/NULL=제한없음 */
    @Column(name = "min_age")
    private Integer minAge;

    /** sprtTrgtMaxAge */
    @Column(name = "max_age")
    private Integer maxAge;

    /** jobCd — 콤마 다중 */
    @Column(name = "job_codes", length = 255)
    private String jobCodes;

    /** schoolCd — 콤마 다중 */
    @Column(name = "school_codes", length = 255)
    private String schoolCodes;

    // ---- 자격: 원문 노출용 ----

    /** earnCndSeCd */
    @Column(name = "income_condition_code", length = 16)
    private String incomeConditionCode;

    /** earnMaxAmt — 연소득 상한(만원) */
    @Column(name = "income_max_amount")
    private Integer incomeMaxAmount;

    /** earnEtcCn */
    @Column(name = "income_etc_content", columnDefinition = "TEXT")
    private String incomeEtcContent;

    /** mrgSttsCd */
    @Column(name = "marital_status_code", length = 16)
    private String maritalStatusCode;

    /** plcyMajorCd — 콤마 다중 */
    @Column(name = "major_codes", length = 255)
    private String majorCodes;

    /** sbizCd — 콤마 다중 */
    @Column(name = "specialization_codes", length = 255)
    private String specializationCodes;

    /** addAplyQlfcCndCn */
    @Column(name = "additional_qualification", columnDefinition = "TEXT")
    private String additionalQualification;

    /** ptcpPrpTrgtCn */
    @Column(name = "participation_restriction", columnDefinition = "TEXT")
    private String participationRestriction;

    // ---- 기간/상태 ----

    /** aplyPrdSeCd */
    @Column(name = "application_period_type", length = 16)
    private String applicationPeriodType;

    /** aplyYmd 원문 */
    @Column(name = "application_period_raw", length = 64)
    private String applicationPeriodRaw;

    /** [파생] 신청 시작일 */
    @Column(name = "application_start_date")
    private LocalDate applicationStartDate;

    /** [파생] 마감일 — D-day 정렬/필터 */
    @Column(name = "application_end_date")
    private LocalDate applicationEndDate;

    /** bizPrdBgngYmd */
    @Column(name = "business_period_begin")
    private LocalDate businessPeriodBegin;

    /** bizPrdEndYmd */
    @Column(name = "business_period_end")
    private LocalDate businessPeriodEnd;

    /** bizPrdEtcCn */
    @Column(name = "business_period_etc", length = 64)
    private String businessPeriodEtc;

    // ---- 규모 ----

    /** sprtSclCnt — 모집인원 */
    @Column(name = "support_scale_count")
    private Integer supportScaleCount;

    /** sprtArvlSeqYn — 선착순 */
    @Column(name = "first_come_first_served", nullable = false)
    private boolean firstComeFirstServed;

    // ---- 링크/신청 ----

    /** aplyUrlAddr */
    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    /** refUrlAddr1 */
    @Column(name = "reference_url1", length = 500)
    private String referenceUrl1;

    /** refUrlAddr2 */
    @Column(name = "reference_url2", length = 500)
    private String referenceUrl2;

    /** plcyAplyMthdCn */
    @Column(name = "application_method", columnDefinition = "TEXT")
    private String applicationMethod;

    /** sbmsnDcmntCn */
    @Column(name = "submission_documents", columnDefinition = "TEXT")
    private String submissionDocuments;

    /** srngMthdCn */
    @Column(name = "screening_method", columnDefinition = "TEXT")
    private String screeningMethod;

    // ---- 보류 필드 (적재만, 판정 사용 금지) ----

    /** [보류] sprtTrgtAgeLmtYn — 본문과 모순 사례. 판정은 min/maxAge만 */
    @Column(name = "age_limit_flag", length = 4)
    private String ageLimitFlag;

    /** [보류] earnMinAmt — 실데이터 거의 0 */
    @Column(name = "income_min_amount")
    private Integer incomeMinAmount;

    /** [보류] sprtSclLmtYn */
    @Column(name = "support_scale_limit")
    private Boolean supportScaleLimit;

    /** [보류] operInstCdNm */
    @Column(name = "operating_institution_name", length = 255)
    private String operatingInstitutionName;

    /** [보류] plcyAprvSttsCd */
    @Column(name = "approval_status_code", length = 16)
    private String approvalStatusCode;

    /** [보류] etcMttrCn */
    @Column(name = "etc_matters", columnDefinition = "TEXT")
    private String etcMatters;

    // ---- 메타/정렬/운영 ----

    /** inqCnt — 인기 정렬 */
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    /** frstRegDt — 최신순 정렬 */
    @Column(name = "first_registered_at")
    private LocalDateTime firstRegisteredAt;

    /** lastMdfcnDt — 변경 감지(upsert 비교) */
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PolicyVisibility visibility = PolicyVisibility.VISIBLE;

    /** 수집에서 안 보인 연속 횟수 — 3회 도달 시 HIDDEN, 재등장 시 0 리셋 (V2, 기획 §6) */
    @Column(name = "missing_count", nullable = false)
    private int missingCount;

    /**
     * [파생] 전 시도 커버 여부 (V13, #120). 지역 필터 조회에서 지역 특화 정책을 먼저 노출하기 위한 정렬 키다.
     *
     * <p>{@link #create} 파라미터에 넣지 않는다 — 전처리(PolicyMapper) 시점에는 zipCd가 아직 Region으로 해석되기 전이라 값을 알 수
     * 없다. {@link #updateFrom}에서도 복사하지 않는다 — source는 지역 정보가 없는 전처리 결과라 항상 false여서, 복사하면 갱신되는 정책이 전부
     * false로 덮인다. 반드시 {@link #applyRegionCoverage}로 지역 저장과 함께 갱신한다.
     */
    @Column(name = "is_nationwide", nullable = false)
    private boolean nationwide;

    private static final int MISSING_THRESHOLD = 3;

    /**
     * 적용 지역 갱신과 반드시 함께 호출한다 — policy_regions와 어긋나면 정렬이 조용히 틀린다. 현재 유일한 호출 지점은
     * PolicyUpsertWriter.writeOne()이며, 지역 행 저장과 같은 트랜잭션 안에 있다.
     */
    public void applyRegionCoverage(boolean nationwide) {
        this.nationwide = nationwide;
    }

    /**
     * id 없는 새 비영속 복사본. 청크 트랜잭션이 롤백돼도 IDENTITY가 이미 부여한 id는 엔티티 객체에 남으므로, 건별 재시도는 원본 재사용 대신 이 복사본으로
     * INSERT해야 한다.
     */
    public static Policy copyOf(Policy source) {
        Policy policy = new Policy();
        policy.updateFrom(source);
        return policy;
    }

    /** 이번 수집에서 안 보였음 — 연속 3회 도달 시 숨김 (즉시 숨김 금지, 기획 §6). */
    public void markMissing() {
        missingCount++;
        if (missingCount >= MISSING_THRESHOLD) {
            visibility = PolicyVisibility.HIDDEN;
        }
    }

    /**
     * 수집된 최신 내용으로 전 필드 갱신 + 누락 상태 리셋(재등장 포함). 필드 나열 순서 = 선언 순서 — {@link #create} 규약과 동일하게
     * PolicyMapperTest/PolicyTest가 누락을 잡는다.
     */
    public void updateFrom(Policy source) {
        this.policyNo = source.policyNo;
        this.title = source.title;
        this.description = source.description;
        this.supportContent = source.supportContent;
        this.keywords = source.keywords;
        this.category = source.category;
        this.middleCategory = source.middleCategory;
        this.organizationName = source.organizationName;
        this.minAge = source.minAge;
        this.maxAge = source.maxAge;
        this.jobCodes = source.jobCodes;
        this.schoolCodes = source.schoolCodes;
        this.incomeConditionCode = source.incomeConditionCode;
        this.incomeMaxAmount = source.incomeMaxAmount;
        this.incomeEtcContent = source.incomeEtcContent;
        this.maritalStatusCode = source.maritalStatusCode;
        this.majorCodes = source.majorCodes;
        this.specializationCodes = source.specializationCodes;
        this.additionalQualification = source.additionalQualification;
        this.participationRestriction = source.participationRestriction;
        this.applicationPeriodType = source.applicationPeriodType;
        this.applicationPeriodRaw = source.applicationPeriodRaw;
        this.applicationStartDate = source.applicationStartDate;
        this.applicationEndDate = source.applicationEndDate;
        this.businessPeriodBegin = source.businessPeriodBegin;
        this.businessPeriodEnd = source.businessPeriodEnd;
        this.businessPeriodEtc = source.businessPeriodEtc;
        this.supportScaleCount = source.supportScaleCount;
        this.firstComeFirstServed = source.firstComeFirstServed;
        this.applicationUrl = source.applicationUrl;
        this.referenceUrl1 = source.referenceUrl1;
        this.referenceUrl2 = source.referenceUrl2;
        this.applicationMethod = source.applicationMethod;
        this.submissionDocuments = source.submissionDocuments;
        this.screeningMethod = source.screeningMethod;
        this.ageLimitFlag = source.ageLimitFlag;
        this.incomeMinAmount = source.incomeMinAmount;
        this.supportScaleLimit = source.supportScaleLimit;
        this.operatingInstitutionName = source.operatingInstitutionName;
        this.approvalStatusCode = source.approvalStatusCode;
        this.etcMatters = source.etcMatters;
        this.viewCount = source.viewCount;
        this.firstRegisteredAt = source.firstRegisteredAt;
        this.lastModifiedAt = source.lastModifiedAt;
        this.rawPayload = source.rawPayload;
        this.missingCount = 0;
        this.visibility = PolicyVisibility.VISIBLE;
    }

    /** API 응답 원문 JSON — 스키마 진화 시 백필용 보험 */
    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    /**
     * 배치 전처리(PolicyMapper) 전용 생성 통로. 파라미터 순서 = 필드 선언 순서 — 인접 String이 많아 순서가 바뀌어도 컴파일은 통과하므로, 필드를
     * 추가/삭제할 때 반드시 선언 순서를 유지하고 PolicyMapperTest의 전 필드 검증으로 확인한다.
     */
    public static Policy create(
            String policyNo,
            String title,
            String description,
            String supportContent,
            String keywords,
            String category,
            String middleCategory,
            String organizationName,
            Integer minAge,
            Integer maxAge,
            String jobCodes,
            String schoolCodes,
            String incomeConditionCode,
            Integer incomeMaxAmount,
            String incomeEtcContent,
            String maritalStatusCode,
            String majorCodes,
            String specializationCodes,
            String additionalQualification,
            String participationRestriction,
            String applicationPeriodType,
            String applicationPeriodRaw,
            LocalDate applicationStartDate,
            LocalDate applicationEndDate,
            LocalDate businessPeriodBegin,
            LocalDate businessPeriodEnd,
            String businessPeriodEtc,
            Integer supportScaleCount,
            boolean firstComeFirstServed,
            String applicationUrl,
            String referenceUrl1,
            String referenceUrl2,
            String applicationMethod,
            String submissionDocuments,
            String screeningMethod,
            String ageLimitFlag,
            Integer incomeMinAmount,
            Boolean supportScaleLimit,
            String operatingInstitutionName,
            String approvalStatusCode,
            String etcMatters,
            int viewCount,
            LocalDateTime firstRegisteredAt,
            LocalDateTime lastModifiedAt,
            String rawPayload) {
        Policy policy = new Policy();
        policy.policyNo = policyNo;
        policy.title = title;
        policy.description = description;
        policy.supportContent = supportContent;
        policy.keywords = keywords;
        policy.category = category;
        policy.middleCategory = middleCategory;
        policy.organizationName = organizationName;
        policy.minAge = minAge;
        policy.maxAge = maxAge;
        policy.jobCodes = jobCodes;
        policy.schoolCodes = schoolCodes;
        policy.incomeConditionCode = incomeConditionCode;
        policy.incomeMaxAmount = incomeMaxAmount;
        policy.incomeEtcContent = incomeEtcContent;
        policy.maritalStatusCode = maritalStatusCode;
        policy.majorCodes = majorCodes;
        policy.specializationCodes = specializationCodes;
        policy.additionalQualification = additionalQualification;
        policy.participationRestriction = participationRestriction;
        policy.applicationPeriodType = applicationPeriodType;
        policy.applicationPeriodRaw = applicationPeriodRaw;
        policy.applicationStartDate = applicationStartDate;
        policy.applicationEndDate = applicationEndDate;
        policy.businessPeriodBegin = businessPeriodBegin;
        policy.businessPeriodEnd = businessPeriodEnd;
        policy.businessPeriodEtc = businessPeriodEtc;
        policy.supportScaleCount = supportScaleCount;
        policy.firstComeFirstServed = firstComeFirstServed;
        policy.applicationUrl = applicationUrl;
        policy.referenceUrl1 = referenceUrl1;
        policy.referenceUrl2 = referenceUrl2;
        policy.applicationMethod = applicationMethod;
        policy.submissionDocuments = submissionDocuments;
        policy.screeningMethod = screeningMethod;
        policy.ageLimitFlag = ageLimitFlag;
        policy.incomeMinAmount = incomeMinAmount;
        policy.supportScaleLimit = supportScaleLimit;
        policy.operatingInstitutionName = operatingInstitutionName;
        policy.approvalStatusCode = approvalStatusCode;
        policy.etcMatters = etcMatters;
        policy.viewCount = viewCount;
        policy.firstRegisteredAt = firstRegisteredAt;
        policy.lastModifiedAt = lastModifiedAt;
        policy.rawPayload = rawPayload;
        return policy;
    }

    /** 관리자 soft delete 시각. {@link #visibility}(배치의 재노출 가능한 상태 전환)와는 독립적이다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void changeVisibility(PolicyVisibility visibility) {
        this.visibility = visibility;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void updateDetails(
            String title,
            String organizationName,
            String description,
            String category,
            String middleCategory,
            LocalDate applicationStartDate,
            LocalDate applicationEndDate,
            String applicationUrl) {
        this.title = title;
        this.organizationName = organizationName;
        this.description = description;
        this.category = category;
        this.middleCategory = middleCategory;
        this.applicationStartDate = applicationStartDate;
        this.applicationEndDate = applicationEndDate;
        this.applicationUrl = applicationUrl;
    }
}
