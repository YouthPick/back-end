package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyApplicationChecklistRepository;
import com.bop.youthpick.policy.repository.PolicyApplicationRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * "부모 신청관리만 소프트 삭제된 고아 체크리스트" 시나리오의 H2 통합 테스트. Mockito 목으로는 JPA LAZY 프록시가
 * {@code @SQLRestriction("deleted_at IS NULL")}에 걸려 {@code EntityNotFoundException}(500)을 던지는 동작이
 * 재현되지 않아서(목은 진짜 엔티티를 돌려주므로 프록시 초기화 자체가 없다), 실제 영속성 컨텍스트 위에서 서비스를 통째로 돌려 검증한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyApplicationChecklistOrphanIntegrationTest {

    @Autowired private PolicyApplicationRepository policyApplicationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyApplicationChecklistRepository applicationChecklistRepository;
    @Autowired private TestEntityManager entityManager;

    private PolicyApplicationService applicationService;
    private PolicyApplicationChecklistService checklistService;

    private Long userId;
    private Long applicationId;
    private Long checklistId;

    @BeforeEach
    void setUp() {
        applicationService =
                new PolicyApplicationService(
                        policyApplicationRepository,
                        userRepository,
                        policyRepository,
                        applicationChecklistRepository);
        checklistService =
                new PolicyApplicationChecklistService(
                        applicationChecklistRepository, applicationService);

        User user = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        userId = user.getId();

        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", "P001");
        ReflectionTestUtils.setField(policy, "title", "청년 일자리 지원");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        entityManager.persistAndFlush(policy);

        PolicyApplication application =
                PolicyApplication.create(user, policy, ApplicationStatus.INTERESTED, null, null);
        entityManager.persistAndFlush(application);
        applicationId = application.getId();

        PolicyApplicationChecklist checklist =
                PolicyApplicationChecklist.create(application, "서류 A");
        entityManager.persistAndFlush(checklist);
        checklistId = checklist.getId();

        // 이후 서비스 호출이 1차 캐시가 아니라 DB 조회(프록시 포함)를 타도록 영속성 컨텍스트를 비운다.
        entityManager.clear();
    }

    @Test
    void 신청을_삭제하면_체크리스트도_함께_soft_delete된다() {
        applicationService.delete(applicationId, userId);
        entityManager.flush();
        entityManager.clear();

        assertThat(
                        applicationChecklistRepository
                                .findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(
                                        applicationId, Pageable.unpaged())
                                .getContent())
                .isEmpty();
        assertThat(applicationChecklistRepository.findActiveWithApplicationById(checklistId))
                .isEmpty();
    }

    @Test
    void 신청_soft_delete_후_체크리스트를_체크하면_CHECKLIST_NOT_FOUND를_던진다() {
        applicationService.delete(applicationId, userId);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> checklistService.check(checklistId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }

    /**
     * 정리 로직이 돌기 전에 이미 만들어진(체크리스트는 살아있고 부모만 소프트 삭제된) 고아 데이터 회귀 테스트. 수정 전 코드에서는 {@code
     * checklist.getApplication()} LAZY 프록시 초기화가 부모의 {@code @SQLRestriction}에 걸려 {@code
     * EntityNotFoundException} → S001 500으로 샜다. 이제는 조회 쿼리가 부모 deleted_at 조건까지 걸어 빈 결과 → P005(404)로
     * 떨어져야 한다.
     */
    @Test
    void 부모만_삭제된_고아_체크리스트를_체크하면_500이_아니라_CHECKLIST_NOT_FOUND를_던진다() {
        PolicyApplication application = entityManager.find(PolicyApplication.class, applicationId);
        ReflectionTestUtils.setField(application, "deletedAt", LocalDateTime.now());
        entityManager.persistAndFlush(application);
        entityManager.clear();

        assertThatThrownBy(() -> checklistService.check(checklistId, userId))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.CHECKLIST_NOT_FOUND);
    }
}
