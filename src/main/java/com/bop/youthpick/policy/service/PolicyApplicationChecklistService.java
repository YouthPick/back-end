package com.bop.youthpick.policy.service;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
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
    public PolicyApplicationChecklist add(Long applicationId, Long userId, String message) {
        PolicyApplication application = findActiveApplication(applicationId, userId);

        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, message);
        return applicationChecklistRepository.save(checklist);
    }

    @Transactional
    public void check(Long id, Long userId) {
        findActive(id, userId).check();
    }

    @Transactional
    public void uncheck(Long id, Long userId) {
        findActive(id, userId).uncheck();
    }

    @Transactional
    public void delete(Long id, Long userId) {
        findActive(id, userId).delete();
    }

    @Transactional(readOnly = true)
    public Page<PolicyApplicationChecklistResponse> getByApplication(
            Long applicationId, Long userId, Pageable pageable) {
        findActiveApplication(applicationId, userId);

        return applicationChecklistRepository
                .findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(applicationId, pageable)
                .map(PolicyApplicationChecklistResponse::from);
    }

    private PolicyApplication findActiveApplication(Long applicationId, Long userId) {
        PolicyApplication application =
                policyApplicationRepository
                        .findByIdAndDeletedAtIsNull(applicationId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
        verifyOwner(application, userId);
        return application;
    }

    private PolicyApplicationChecklist findActive(Long id, Long userId) {
        PolicyApplicationChecklist checklist =
                applicationChecklistRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(
                                () -> new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND));
        if (checklist.getApplication().isDeleted()) {
            throw new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND);
        }
        verifyOwner(checklist.getApplication(), userId);
        return checklist;
    }

    private void verifyOwner(PolicyApplication application, Long userId) {
        if (!application.getUser().getId().equals(userId)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }
}
