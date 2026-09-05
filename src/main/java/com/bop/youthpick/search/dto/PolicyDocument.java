package com.bop.youthpick.search.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Elasticsearch 색인 문서. 필드 구성은 {@code es/policy-index.json} 매핑과 1:1로 맞춘다
 * (매핑이 {@code dynamic: "strict"}라 여기에만 있는 필드를 보내면 색인이 400으로 거부된다).
 *
 * <p>{@link Policy}를 통째로 보내지 않는 이유: ES는 저장소가 아니라 <b>검색 인덱스</b>로만 쓴다. 검색 결과의 카드 내용은
 * 기존대로 MySQL에서 읽으므로, ES에는 "어떤 정책이 걸리고 어떤 순서인가"를 판정할 필드만 넣는다.
 * rawPayload(LONGTEXT)·제출서류·심사방법 등은 색인만 키우고 검색에 쓰이지 않는다.
 *
 * <p>{@code sidoNames}는 policy_regions 조인 대신 배열로 비정규화한 것이다 — ES에는 JOIN이 없다. 대신 기존
 * {@code findCards}의 지역 EXISTS 서브쿼리가 {@code terms} 필터 한 줄로 줄어든다.
 */
public record PolicyDocument(
    Long policyId,

    // ── 검색 대상 (text, 한국어 분석기로 쪼개서 색인) ──
    String title,
    String keywords,
    String description,
    String supportContent,
    String organizationName,

    // ── 필터·집계 대상 (keyword, 쪼개지 않고 정확 일치) ──
    String category,
    List<String> sidoNames,
    List<String> jobCodes,
    String visibility,

    // ── 나이 겹침 판정 ──
    Integer minAge,
    Integer maxAge,

    // ── 마감 조건 ──
    LocalDate applicationEndDate,
    LocalDate businessPeriodEnd,
    LocalDate firstRegisteredAt,

    // ── 정렬·제외 ──
    boolean nationwide,
    boolean adminHidden) {

    /** 색인 문서 id. MySQL PK를 그대로 쓴다 — 같은 정책을 다시 색인하면 새로 쌓이지 않고 덮어써진다. */
    public String documentId() {
        return String.valueOf(policyId);
    }

    public static PolicyDocument from(Policy policy, List<String> sidoNames) {
        return new PolicyDocument(
            policy.getId(),
            policy.getTitle(),
            policy.getKeywords(),
            policy.getDescription(),
            policy.getSupportContent(),
            policy.getOrganizationName(),
            policy.getCategory(),
            sidoNames,
            splitCodes(policy.getJobCodes()),
            policy.getVisibility().name(),
            policy.getMinAge(),
            policy.getMaxAge(),
            policy.getApplicationEndDate(),
            policy.getBusinessPeriodEnd(),
            policy.getFirstRegisteredAt() == null
                ? null
                : policy.getFirstRegisteredAt().toLocalDate(),
            policy.isNationwide(),
            policy.isAdminHidden());
    }

    /**
     * DB에 콤마로 이어 붙여 저장된 취업상태 코드를 배열로 편다. MySQL에서는
     * {@code concat(',', jobCodes, ',') like '%,코드,%'}로 부분일치를 흉내 냈는데, ES에서는 배열 필드에
     * {@code terms} 필터를 걸면 되므로 문자열 조작이 사라진다.
     */
    private static List<String> splitCodes(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return Arrays.stream(commaSeparated.split(","))
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .toList();
    }
}
