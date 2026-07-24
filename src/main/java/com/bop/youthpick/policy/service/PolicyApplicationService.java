package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PolicyApplicationService {

    private final PolicyApplicationRepository policyApplicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyApplicationChecklistRepository policyApplicationChecklistRepository;

    /**
     * 정책 신청관리를 새로 등록한다. 같은 (user, policy) 조합의 기존 행이 있으면 두 갈래로 나뉜다: soft-delete되지 않은 상태면 {@link
     * PolicyErrorCode#POLICY_ALREADY_EXISTS}, soft-delete된 상태면 {@link
     * PolicyApplication#reactivate}로 같은 행을 재활성화하고(상태/메모/마감일을 이번 요청 값으로 덮어씀) 기존에 연결돼 있던 체크리스트를 모두
     * 소프트삭제한다.
     */
    @Transactional
    public PolicyApplication create(
            Long userId,
            Long policyId,
            ApplicationStatus status,
            String memo,
            LocalDateTime endAt) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        Policy policy =
                policyRepository
                        .findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                                policyId, PolicyVisibility.VISIBLE)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
        PolicyApplication existing =
                policyApplicationRepository
                        .findIncludingDeletedByUserIdAndPolicyId(userId, policyId)
                        .orElse(null);
        String normalizedMemo = blankToNull(memo);

        if (existing != null) {
            if (!existing.isDeleted()) {
                throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
            }
            existing.reactivate(status, normalizedMemo, resolveEndAt(endAt, policy));
            policyApplicationChecklistRepository.softDeleteAllByApplicationId(existing.getId());
            return existing;
        }
        PolicyApplication application =
                PolicyApplication.create(
                        user, policy, status, normalizedMemo, resolveEndAt(endAt, policy));

        try {
            return policyApplicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            // including-deleted 조회 확인 이후 동시 요청이 먼저 저장한 경우
            // (uk_policy_applications_user_policy UNIQUE 위반). 같은 도메인 에러로 통일한다.
            throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
        }
    }

    /** 신청관리 항목의 상태만 단독으로 변경한다. 소유권 검증 후 변경하며, 트랜잭션 커밋 시 dirty checking으로 자동 반영된다. */
    @Transactional
    public PolicyApplication changeStatus(Long id, Long userId, ApplicationStatus status) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.changeStatus(status);
        return application;
    }

    /** 신청관리 항목의 개인 메모만 단독으로 수정한다. 공백/빈 문자열은 {@link #blankToNull}로 null 정규화한다. */
    @Transactional
    public PolicyApplication updateMemo(Long id, Long userId, String memo) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.updateMemo(blankToNull(memo));
        return application;
    }

    /** 신청관리 항목의 개인 마감일만 단독으로 수정한다. null이 아니면 정책 자체의 신청 마감일을 넘지 않는지 검증한다. */
    @Transactional
    public PolicyApplication updateEndAt(Long id, Long userId, LocalDateTime endAt) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        if (endAt != null) {
            validateWithinPolicyDeadline(endAt, application.getPolicy());
        }
        application.updateEndAt(endAt);
        return application;
    }

    /** 로그인 사용자의 삭제되지 않은 신청관리 목록을 페이지로 조회한다. */
    @Transactional(readOnly = true)
    public Page<PolicyApplicationResponse> getApplications(Long userId, Pageable pageable) {
        return policyApplicationRepository
                .findByUser_IdAndDeletedAtIsNull(userId, pageable)
                .map(PolicyApplicationResponse::from);
    }

    /**
     * 신청관리 항목을 소프트 삭제(관심 해제)한다. 소유권 검증 후 처리하며, 딸린 체크리스트도 {@link
     * PolicyApplicationChecklistRepository#softDeleteAllByApplicationId}로 함께 소프트 삭제한다 — 신청만 지우고
     * 체크리스트를 남기면 "고아" 체크리스트가 생기고, "신청 삭제 후 재등록 시 체크리스트 초기화"라는 {@link #create}의 reactivate 분기 스펙과도
     * 어긋나기 때문이다.
     */
    @Transactional
    public void delete(Long id, Long userId) {
        PolicyApplication application = findActive(id);
        application.verifyOwner(userId);
        application.delete();
        policyApplicationChecklistRepository.softDeleteAllByApplicationId(id);
    }

    /**
     * id로 조회하되 soft-delete된 행은 제외한다. 없으면 {@link PolicyErrorCode#POLICY_APPLICATION_NOT_FOUND}. 접근
     * 제한자가 package-private인 이유: 같은 패키지의 {@link PolicyApplicationChecklistService}도 부모 신청관리를 조회할 때 이
     * 메서드를 그대로 재사용한다(두 서비스가 각자 같은 Repository 조회를 중복 구현하던 것을 여기로 합쳤다).
     */
    PolicyApplication findActive(Long id) {
        return policyApplicationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
    }

    /** 메모가 비어있거나 공백이면 null로 통일하고, 값이 있으면 원본 그대로(가공 없이) 반환한다. */
    private static String blankToNull(String memo) {
        return StringUtils.hasText(memo) ? memo : null;
    }

    /** 개인이 마감일을 지정하지 않으면(null) 정책 자체의 신청 마감일을 기본값으로 잡는다. 직접 지정한 경우엔 정책 마감일을 넘지 않는지 검증한다. */
    private static LocalDateTime resolveEndAt(LocalDateTime requestedEndAt, Policy policy) {
        if (requestedEndAt == null) {
            return policy.getApplicationEndDate() != null
                    ? policy.getApplicationEndDate().atStartOfDay()
                    : null;
        }
        validateWithinPolicyDeadline(requestedEndAt, policy);
        return requestedEndAt;
    }

    /** 정책 자체의 신청 마감일이 알려져 있다면(null이 아니면), 개인 마감일이 그 날짜를 넘지 않아야 한다. */
    private static void validateWithinPolicyDeadline(LocalDateTime endAt, Policy policy) {
        if (policy.getApplicationEndDate() != null
                && endAt.toLocalDate().isAfter(policy.getApplicationEndDate())) {
            throw new CustomException(PolicyErrorCode.END_AT_AFTER_POLICY_DEADLINE);
        }
    }
}
