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

/**
 * 정책 신청관리(PolicyApplication)에 딸린 준비 체크리스트의 비즈니스 로직. 체크리스트는 항상 부모 PolicyApplication을 통해서만 접근 가능하다 —
 * applicationId 하나 없이 체크리스트만 단독 조회하는 API는 없다. PolicyApplicationRepository를 직접 참조하는 것도 그래서다(체크리스트 자체
 * 레포만으론 소유권을 못 가린다).
 */
@Service
@RequiredArgsConstructor
public class PolicyApplicationChecklistService {

    private final PolicyApplicationChecklistRepository applicationChecklistRepository;
    private final PolicyApplicationRepository policyApplicationRepository;

    // PolicyApplicationChecklistController.add()에서 호출. 먼저 findActiveApplication()으로 부모 신청관리가
    // 존재하고 요청자 소유인지 확인한 뒤에만 PolicyApplicationChecklist.create()로 새 항목을 만든다.
    @Transactional
    public PolicyApplicationChecklist add(Long applicationId, Long userId, String message) {
        PolicyApplication application = findActiveApplication(applicationId, userId);

        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, message);
        return applicationChecklistRepository.save(checklist);
    }

    // PolicyApplicationChecklistController.update()에서 호출. save() 없이 필드만 바꿔도 @Transactional
    // 커밋 시 dirty checking으로 자동 반영된다(PolicyApplication.changeStatus()와 같은 패턴).
    @Transactional
    public PolicyApplicationChecklist update(Long id, Long userId, String message) {
        PolicyApplicationChecklist checklist = findActive(id, userId);
        checklist.updateContent(message);
        return checklist;
    }

    // PolicyApplicationChecklistController.check()에서 호출.
    @Transactional
    public void check(Long id, Long userId) {
        findActive(id, userId).check();
    }

    // PolicyApplicationChecklistController.uncheck()에서 호출.
    @Transactional
    public void uncheck(Long id, Long userId) {
        findActive(id, userId).uncheck();
    }

    // PolicyApplicationChecklistController.delete()에서 호출 — 항목 하나만 소프트 삭제.
    // 부모 신청관리 전체가 삭제/재등록될 때 체크리스트를 일괄 정리하는 건 여기가 아니라
    // PolicyApplicationChecklistRepository.softDeleteAllByApplicationId()이고, 그건
    // PolicyApplicationService.register()의 reactivate 분기에서 (이 서비스를 거치지 않고) 직접 호출된다.
    @Transactional
    public void delete(Long id, Long userId) {
        findActive(id, userId).delete();
    }

    // PolicyApplicationChecklistController.getByApplication()에서 호출. 부모 신청관리 소유권 확인(1쿼리) 후
    // 체크리스트 목록을 id 오름차순으로 조회(1쿼리)해 PolicyApplicationChecklistResponse로 변환한다.
    @Transactional(readOnly = true)
    public Page<PolicyApplicationChecklistResponse> getByApplication(
            Long applicationId, Long userId, Pageable pageable) {
        findActiveApplication(applicationId, userId);

        return applicationChecklistRepository
                .findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(applicationId, pageable)
                .map(PolicyApplicationChecklistResponse::from);
    }

    // add()/getByApplication()이 공통으로 쓰는 "부모 신청관리가 존재하고, 삭제되지 않았고, 요청자 소유인지" 확인.
    // PolicyApplicationService.findActive()와 거의 같은 조회(findByIdAndDeletedAtIsNull)를 이 서비스가
    // 별도로 다시 한다 — 두 서비스가 같은 Repository를 각자 호출하는 구조라 로직이 갈라져 있다.
    private PolicyApplication findActiveApplication(Long applicationId, Long userId) {
        PolicyApplication application =
                policyApplicationRepository
                        .findByIdAndDeletedAtIsNull(applicationId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));
        application.verifyOwner(userId);
        return application;
    }

    // update/check/uncheck/delete가 공통으로 쓰는 조회. 체크리스트 자신의 deletedAt뿐 아니라
    // checklist.getApplication().isDeleted()까지 한 번 더 확인하는 이유: 체크리스트는 안 지워졌는데 부모
    // PolicyApplication만 나중에 소프트 삭제된 경우를 걸러내기 위해서다(그런 상태의 체크리스트는 "고아"로 취급해
    // CHECKLIST_NOT_FOUND로 통일).
    private PolicyApplicationChecklist findActive(Long id, Long userId) {
        PolicyApplicationChecklist checklist =
                applicationChecklistRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(
                                () -> new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND));
        if (checklist.getApplication().isDeleted()) {
            throw new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND);
        }
        checklist.getApplication().verifyOwner(userId);
        return checklist;
    }
}
