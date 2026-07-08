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

    /** API 응답 원문 JSON — 스키마 진화 시 백필용 보험 */
    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;
}
