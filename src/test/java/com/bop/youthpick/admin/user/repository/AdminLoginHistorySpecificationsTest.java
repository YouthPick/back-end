package com.bop.youthpick.admin.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.user.entity.LoginHistory;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.LoginHistoryRepository;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminLoginHistorySpecificationsTest {

    @Autowired private LoginHistoryRepository loginHistoryRepository;

    @Autowired private UserRepository userRepository;

    @Autowired private TestEntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        User otherUser =
                userRepository.save(User.createSocialUser("naver", "naver-1", null, "다른닉네임"));
        userId = user.getId();

        saveLoginHistory(user, LocalDateTime.of(2026, 3, 15, 9, 0));
        saveLoginHistory(user, LocalDateTime.of(2026, 6, 1, 9, 0));
        saveLoginHistory(otherUser, LocalDateTime.of(2026, 6, 10, 9, 0));
    }

    // createdAt은 @CreatedDate + updatable=false라 auditing이 채운 뒤에는 JPA로 값을 바꿀 수 없다.
    // 날짜 필터 테스트를 위해 네이티브 쿼리로 직접 덮어쓴다.
    private void saveLoginHistory(User user, LocalDateTime createdAt) {
        LoginHistory saved = loginHistoryRepository.saveAndFlush(LoginHistory.create(user));

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE login_histories SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void userId로_필터링한다() {
        List<LoginHistory> result =
                loginHistoryRepository.findAll(
                        AdminLoginHistorySpecifications.filter(userId, null, null));

        assertThat(result).hasSize(2);
    }

    @Test
    void 날짜범위로_필터링한다() {
        List<LoginHistory> result =
                loginHistoryRepository.findAll(
                        AdminLoginHistorySpecifications.filter(
                                null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(result).hasSize(1);
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<LoginHistory> result =
                loginHistoryRepository.findAll(
                        AdminLoginHistorySpecifications.filter(null, null, null));

        assertThat(result).hasSize(3);
    }
}
