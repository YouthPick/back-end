package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정책 신청관리(PolicyApplication)에 딸린 준비 체크리스트의 비즈니스 로직. 체크리스트는 항상 부모 PolicyApplication을 통해서만 접근 가능하다 —
 * applicationId 하나 없이 체크리스트만 단독 조회하는 API는 없다. 부모 신청관리 조회는 PolicyApplicationRepository를 이 서비스가 직접 다시
 * 부르지 않고 {@link PolicyApplicationService#findActive}를 재사용한다(체크리스트 자체 레포만으론 소유권을 못 가려 여전히 부모 조회가
 * 필요하지만, 그 조회 자체는 한 곳에서만 구현한다).
 */
@Service
@RequiredArgsConstructor
public class PolicyApplicationChecklistService {

    private final PolicyApplicationChecklistRepository applicationChecklistRepository;
    private final PolicyApplicationService policyApplicationService;

    /**
     * 먼저 {@link #findActiveApplication}으로 부모 신청관리가 존재하고 요청자 소유인지 확인한 뒤에만 {@link
     * PolicyApplicationChecklist#create}로 새 항목을 만든다.
     */
    @Transactional
    public PolicyApplicationChecklist add(Long applicationId, Long userId, String message) {
        PolicyApplication application = findActiveApplication(applicationId, userId);

        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, message);
        return applicationChecklistRepository.save(checklist);
    }

    /**
     * {@code save()} 없이 필드만 바꿔도 {@code @Transactional} 커밋 시 dirty checking으로 자동 반영된다 ({@link
     * PolicyApplication#changeStatus}와 같은 패턴).
     */
    @Transactional
    public PolicyApplicationChecklist update(Long id, Long userId, String message) {
        PolicyApplicationChecklist checklist = findActive(id, userId);
        checklist.updateContent(message);
        return checklist;
    }

    @Transactional
    public void check(Long id, Long userId) {
        findActive(id, userId).check();
    }

    @Transactional
    public void uncheck(Long id, Long userId) {
        findActive(id, userId).uncheck();
    }

    /**
     * 항목 하나만 소프트 삭제한다. 부모 신청관리 전체가 삭제/재등록될 때 체크리스트를 일괄 정리하는 건 여기가 아니라 {@link
     * PolicyApplicationChecklistRepository#softDeleteAllByApplicationId}이고, 그건 {@link
     * PolicyApplicationService#create}의 reactivate 분기와 {@link PolicyApplicationService#delete}에서 (이
     * 서비스를 거치지 않고) 직접 호출된다.
     */
    @Transactional
    public void delete(Long id, Long userId) {
        findActive(id, userId).delete();
    }

    /** 부모 신청관리 소유권 확인(1쿼리) 후 체크리스트 목록을 id 오름차순으로 조회(1쿼리)해 응답 DTO로 변환한다. */
    @Transactional(readOnly = true)
    public Page<PolicyApplicationChecklistResponse> getByApplication(
            Long applicationId, Long userId, Pageable pageable) {
        findActiveApplication(applicationId, userId);

        return applicationChecklistRepository
                .findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(applicationId, pageable)
                .map(PolicyApplicationChecklistResponse::from);
    }

    /**
     * {@link #add}/{@link #getByApplication}이 공통으로 쓰는 "부모 신청관리가 존재하고, 삭제되지 않았고, 요청자 소유인지" 확인. 존재/삭제
     * 여부 조회는 {@link PolicyApplicationService#findActive}에 위임하고, 여기선 체크리스트 쪽에서만 필요한 소유권 검증만 이어서 한다.
     */
    private PolicyApplication findActiveApplication(Long applicationId, Long userId) {
        PolicyApplication application = policyApplicationService.findActive(applicationId);
        application.verifyOwner(userId);
        return application;
    }

    /**
     * update/check/uncheck/delete가 공통으로 쓰는 조회. 체크리스트 자신의 {@code deletedAt}과 부모 PolicyApplication의
     * {@code deletedAt}을 리포지토리 쿼리({@code join fetch} + 명시적 deleted_at 조건)에서 한 번에 확인한다 — 부모만 소프트 삭제된
     * "고아" 체크리스트도 CHECKLIST_NOT_FOUND로 통일된다. 부모를 서비스 코드에서 {@code isDeleted()}로 검사하지 않는 이유: LAZY 프록시
     * 초기화 시점에 부모의 {@code @SQLRestriction}에 걸려 검사에 도달하기 전에 {@code EntityNotFoundException}(500)이 터지기
     * 때문이다(그 검사는 도달 불가능한 죽은 코드였다).
     */
    private PolicyApplicationChecklist findActive(Long id, Long userId) {
        PolicyApplicationChecklist checklist =
                applicationChecklistRepository
                        .findActiveWithApplicationById(id)
                        .orElseThrow(
                                () -> new CustomException(PolicyErrorCode.CHECKLIST_NOT_FOUND));
        checklist.getApplication().verifyOwner(userId);
        return checklist;
    }
}
