package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PolicyApplicationController의 모든 엔드포인트가 응답으로 쓰는 DTO — {@link PolicyApplication} 엔티티를 컨트롤러가 직접 반환하지
 * 않기 위한 변환 계층이다(api-design.md의 "Entity를 API 응답으로 직접 반환하지 않는다" 규칙).
 * policyTitle/policyCategory/policyApplicationEndDate는 PolicyApplication 자신의 필드가 아니라 연관된 {@code
 * Policy} 엔티티에서 가져온 값 — 프론트가 신청 목록 카드에 정책 제목/카테고리/마감일을 같이 보여줄 수 있게 한 번에 평탄화(flatten)한 것이다.
 */
public record PolicyApplicationResponse(
        Long id,
        Long policyId,
        String policyTitle,
        String policyCategory,
        LocalDate policyApplicationEndDate,
        ApplicationStatus status,
        String memo,
        LocalDateTime endAt,
        LocalDateTime createdAt) {

    /**
     * PolicyApplicationController의 create/changeStatus/updateMemo/updateEndAt과
     * PolicyApplicationService.getApplications()(Page.map)에서 호출된다. {@code application.getPolicy()}는
     * {@code PolicyApplicationRepository.findByUser_IdAndDeletedAtIsNull()}의
     * {@code @EntityGraph(attributePaths = "policy")} 덕분에 지연 로딩(N+1) 없이 이미 로드돼 있다.
     */
    public static PolicyApplicationResponse from(PolicyApplication application) {
        var policy = application.getPolicy();
        return new PolicyApplicationResponse(
                application.getId(),
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getApplicationEndDate(),
                application.getStatus(),
                application.getMemo(),
                application.getEndAt(),
                application.getCreatedAt());
    }
}
