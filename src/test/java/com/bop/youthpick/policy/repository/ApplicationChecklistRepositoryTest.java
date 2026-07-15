package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
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
class ApplicationChecklistRepositoryTest {

    @Autowired private ApplicationChecklistRepository applicationChecklistRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private TestEntityManager entityManager;

    private Long applicationId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", "P001");
        ReflectionTestUtils.setField(policy, "title", "청년 일자리 지원");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        entityManager.persistAndFlush(policy);

        PolicyApplication application = BeanUtils.instantiateClass(PolicyApplication.class);
        ReflectionTestUtils.setField(application, "user", user);
        ReflectionTestUtils.setField(application, "policy", policy);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.INTERESTED);
        entityManager.persistAndFlush(application);
        applicationId = application.getId();

        saveChecklistItem(application, "서류 A");
        saveChecklistItem(application, "서류 B");
        saveChecklistItem(application, "서류 C");
    }

    private void saveChecklistItem(PolicyApplication application, String content) {
        ApplicationChecklist item = BeanUtils.instantiateClass(ApplicationChecklist.class);
        ReflectionTestUtils.setField(item, "application", application);
        ReflectionTestUtils.setField(item, "content", content);
        ReflectionTestUtils.setField(item, "checked", false);
        entityManager.persistAndFlush(item);
    }

    @Test
    void 체크리스트는_id_오름차순으로_조회된다() {
        List<ApplicationChecklist> result =
                applicationChecklistRepository.findByApplicationIdOrderByIdAsc(applicationId);

        assertThat(result)
                .extracting(ApplicationChecklist::getContent)
                .containsExactly("서류 A", "서류 B", "서류 C");
    }

    @Test
    void 관리_해제된_체크리스트는_조회에서_제외된다() {
        ApplicationChecklist target =
                applicationChecklistRepository
                        .findByApplicationIdOrderByIdAsc(applicationId)
                        .get(0);
        ReflectionTestUtils.setField(target, "deletedAt", LocalDateTime.now());
        entityManager.persistAndFlush(target);
        entityManager.clear();

        List<ApplicationChecklist> result =
                applicationChecklistRepository.findByApplicationIdOrderByIdAsc(applicationId);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ApplicationChecklist::getContent)
                .containsExactly("서류 B", "서류 C");
    }
}
