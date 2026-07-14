package com.bop.youthpick.policy.service;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
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

@Service
@RequiredArgsConstructor
public class PolicyApplicationService {

    private final PolicyApplicationRepository policyApplicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyApplicationChecklistRepository policyApplicationChecklistRepository;

    @Transactional
    public PolicyApplication register(
            Long userId,
            Long policyId,
            ApplicationStatus status,
            String memo,
            LocalDateTime endAt) {
        PolicyApplication existing =
                policyApplicationRepository
                        .findByUser_IdAndPolicy_Id(userId, policyId)
                        .orElse(null);

        if (existing != null) {
            if (!existing.isDeleted()) {
                throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
            }
            existing.reactivate(status, memo, endAt);
            policyApplicationChecklistRepository.softDeleteAllByApplicationId(existing.getId());
            return existing;
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        Policy policy =
                policyRepository
                        .findById(policyId)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        PolicyApplication application =
                PolicyApplication.register(user, policy, status, memo, endAt);
        try {
            return policyApplicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            // findByUser_IdAndPolicy_Id 확인 이후 동시 요청이 먼저 저장한 경우
            // (uk_policy_applications_user_policy UNIQUE 위반). 같은 도메인 에러로 통일한다.
            throw new CustomException(PolicyErrorCode.POLICY_ALREADY_EXISTS);
        }
    }

    @Transactional
    public PolicyApplication changeStatus(Long id, Long userId, ApplicationStatus status) {
        PolicyApplication application = findActive(id);
        verifyOwner(application, userId);
        application.changeStatus(status);
        return application;
    }

    @Transactional
    public PolicyApplication updateMemo(Long id, Long userId, String memo) {
        PolicyApplication application = findActive(id);
        verifyOwner(application, userId);
        application.updateMemo(memo);
        return application;
    }

    @Transactional
    public PolicyApplication updateEndAt(Long id, Long userId, LocalDateTime endAt) {
        PolicyApplication application = findActive(id);
        verifyOwner(application, userId);
        application.updateEndAt(endAt);
        return application;
    }

    @Transactional(readOnly = true)
    public Page<PolicyApplicationResponse> getApplications(Long userId, Pageable pageable) {
        return policyApplicationRepository
                .findByUser_IdAndDeletedAtIsNull(userId, pageable)
                .map(PolicyApplicationResponse::from);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        PolicyApplication application = findActive(id);
        verifyOwner(application, userId);
        application.delete();
    }

    private PolicyApplication findActive(Long id) {
        return policyApplicationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
    }

    private void verifyOwner(PolicyApplication application, Long userId) {
        if (!application.getUser().getId().equals(userId)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }
}
