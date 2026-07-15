package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.AdminPolicyApplicationResponse;
import com.bop.youthpick.policy.dto.ApplicationChecklistItemResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.AdminPolicyApplicationSpecifications;
import com.bop.youthpick.policy.repository.ApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPolicyApplicationService {

    private final PolicyApplicationRepository policyApplicationRepository;
    private final ApplicationChecklistRepository applicationChecklistRepository;

    @Transactional(readOnly = true)
    public Page<AdminPolicyApplicationResponse> search(
            Long userId,
            String policyName,
            String status,
            LocalDate deadlineStart,
            LocalDate deadlineEnd,
            Pageable pageable) {
        return policyApplicationRepository
                .findAll(
                        AdminPolicyApplicationSpecifications.filter(
                                userId, policyName, status, deadlineStart, deadlineEnd),
                        pageable)
                .map(AdminPolicyApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ApplicationChecklistItemResponse> getChecklist(Long applicationId) {
        findApplication(applicationId);
        return applicationChecklistRepository
                .findByApplicationIdOrderByIdAsc(applicationId)
                .stream()
                .map(ApplicationChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public AdminPolicyApplicationResponse updateStatus(Long applicationId, String status) {
        PolicyApplication application = findApplication(applicationId);
        application.changeStatus(parseStatus(status));
        return AdminPolicyApplicationResponse.from(application);
    }

    private PolicyApplication findApplication(Long applicationId) {
        return policyApplicationRepository
                .findById(applicationId)
                .orElseThrow(
                        () -> new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
    }

    private ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(PolicyErrorCode.INVALID_APPLICATION_STATUS);
        }
    }
}
