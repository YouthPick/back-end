package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.ApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyManagementCheckpointService {

    private final PolicyApplicationChecklistRepository applicationChecklistRepository;
    private final PolicyApplicationRepository policyApplicationRepository;

    @Transactional
    public ApplicationChecklist add(Long managementId, String message) {
        PolicyApplication application =
                policyApplicationRepository
                        .findByIdAndDeletedAtIsNull(managementId)
                        .orElseThrow(
                                () -> new CustomException(PolicyErrorCode.MANAGEMENT_NOT_FOUND));

        ApplicationChecklist checklist = ApplicationChecklist.create(application, message);
        return applicationChecklistRepository.save(checklist);
    }

    @Transactional
    public void check(Long id) {
        findActive(id).check();
    }

    @Transactional
    public void uncheck(Long id) {
        findActive(id).uncheck();
    }

    @Transactional
    public void delete(Long id) {
        findActive(id).delete();
    }

    @Transactional(readOnly = true)
    public Page<ApplicationChecklistResponse> getByManagement(
            Long managementId, Pageable pageable) {
        policyApplicationRepository
                .findByIdAndDeletedAtIsNull(managementId)
                .orElseThrow(() -> new CustomException(PolicyErrorCode.MANAGEMENT_NOT_FOUND));

        return applicationChecklistRepository
                .findByApplication_IdAndDeletedAtIsNull(managementId, pageable)
                .map(ApplicationChecklistResponse::from);
    }

    private ApplicationChecklist findActive(Long id) {
        ApplicationChecklist checklist =
                applicationChecklistRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(
                                () -> new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND));
        if (checklist.getApplication().isDeleted()) {
            throw new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND);
        }
        return checklist;
    }
}
