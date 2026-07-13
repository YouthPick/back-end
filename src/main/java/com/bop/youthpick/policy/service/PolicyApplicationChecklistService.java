package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
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
public class PolicyApplicationChecklistService {

    private final PolicyApplicationChecklistRepository applicationChecklistRepository;
    private final PolicyApplicationRepository policyApplicationRepository;

    @Transactional
    public PolicyApplicationChecklist add(Long applicationId, String message) {
        PolicyApplication application =
                policyApplicationRepository
                        .findByIdAndDeletedAtIsNull(applicationId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));

        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, message);
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
    public Page<PolicyApplicationChecklistResponse> getByApplication(
            Long applicationId, Pageable pageable) {
        policyApplicationRepository
                .findByIdAndDeletedAtIsNull(applicationId)
                .orElseThrow(
                        () -> new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));

        return applicationChecklistRepository
                .findByApplication_IdAndDeletedAtIsNull(applicationId, pageable)
                .map(PolicyApplicationChecklistResponse::from);
    }

    private PolicyApplicationChecklist findActive(Long id) {
        PolicyApplicationChecklist checklist =
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
