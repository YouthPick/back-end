package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminPolicyApplicationSpecificationsTest {

    @Autowired private PolicyApplicationRepository policyApplicationRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private TestEntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        userId = user.getId();

        Policy jobPolicy = entityManager.persistAndFlush(newPolicy("P001", "청년 일자리 지원"));
        Policy housingPolicy = entityManager.persistAndFlush(newPolicy("P002", "청년 주거 지원"));
        Policy educationPolicy = entityManager.persistAndFlush(newPolicy("P003", "청년 교육 지원"));

        policyApplicationRepository.save(
                newApplication(
                        user,
                        jobPolicy,
                        ApplicationStatus.INTERESTED,
                        LocalDateTime.of(2026, 3, 15, 0, 0)));
        policyApplicationRepository.save(
                newApplication(
                        user,
                        housingPolicy,
                        ApplicationStatus.SUBMITTED,
                        LocalDateTime.of(2026, 6, 1, 0, 0)));
        policyApplicationRepository.save(
                newApplication(user, educationPolicy, ApplicationStatus.CLOSED, null));
    }

    private Policy newPolicy(String policyNo, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", title);
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }

    private PolicyApplication newApplication(
            User user, Policy policy, ApplicationStatus status, LocalDateTime endAt) {
        PolicyApplication application = BeanUtils.instantiateClass(PolicyApplication.class);
        ReflectionTestUtils.setField(application, "user", user);
        ReflectionTestUtils.setField(application, "policy", policy);
        ReflectionTestUtils.setField(application, "status", status);
        ReflectionTestUtils.setField(application, "endAt", endAt);
        return application;
    }

    @Test
    void userId로_필터링한다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(
                                userId, null, null, null, null));

        assertThat(result).hasSize(3);
    }

    @Test
    void policyName_부분일치로_필터링한다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(null, "주거", null, null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPolicy().getTitle()).isEqualTo("청년 주거 지원");
    }

    @Test
    void status로_필터링한다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(
                                null, null, "CLOSED", null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ApplicationStatus.CLOSED);
    }

    @Test
    void deadline_범위로_필터링한다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(
                                null,
                                null,
                                null,
                                LocalDate.of(2026, 3, 1),
                                LocalDate.of(2026, 3, 31)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ApplicationStatus.INTERESTED);
    }

    @Test
    void deadline이_없는_신청은_기간_필터에서_제외된다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(
                                null,
                                null,
                                null,
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 12, 31)));

        assertThat(result).hasSize(2);
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<PolicyApplication> result =
                policyApplicationRepository.findAll(
                        AdminPolicyApplicationSpecifications.filter(null, null, null, null, null));

        assertThat(result).hasSize(3);
    }
}
