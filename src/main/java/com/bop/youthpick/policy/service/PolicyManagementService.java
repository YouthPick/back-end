package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyManagementService {

    private final PolicyApplicationRepository policyApplicationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;

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
        return policyApplicationRepository.save(application);
    }

    @Transactional
    public PolicyApplication changeStatus(Long id, ApplicationStatus status) {
        PolicyApplication application = findActive(id);
        application.changeStatus(status);
        return application;
    }

    @Transactional(readOnly = true)
    public List<PolicyApplicationResponse> getManagements(Long userId) {
        return policyApplicationRepository.findByUser_IdAndDeletedAtIsNull(userId).stream()
                .map(PolicyApplicationResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        findActive(id).delete();
    }

    private PolicyApplication findActive(Long id) {
        return policyApplicationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new CustomException(PolicyErrorCode.MANAGEMENT_NOT_FOUND));
    }
}
