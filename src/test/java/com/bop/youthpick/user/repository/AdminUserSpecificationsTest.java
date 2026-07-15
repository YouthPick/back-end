package com.bop.youthpick.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminUserSpecificationsTest {

    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User admin = User.createSocialUser("kakao", "kakao-1", "admin@a.com", "관리자");
        admin.changeRole(Role.ADMIN);
        userRepository.save(admin);

        User activeUser = User.createSocialUser("naver", "naver-1", "user@a.com", "회원");
        userRepository.save(activeUser);

        User deletedUser = User.createSocialUser("kakao", "kakao-2", "deleted@a.com", "탈퇴회원");
        deletedUser.softDelete();
        userRepository.save(deletedUser);
    }

    @Test
    void role로_필터링한다() {
        List<User> result =
                userRepository.findAll(AdminUserSpecifications.filter("ADMIN", null, null));

        assertThat(result).extracting(User::getProviderId).containsExactly("kakao-1");
    }

    @Test
    void accountStatus로_필터링한다() {
        List<User> deleted =
                userRepository.findAll(AdminUserSpecifications.filter(null, "DELETED", null));
        List<User> active =
                userRepository.findAll(AdminUserSpecifications.filter(null, "ACTIVE", null));

        assertThat(deleted).extracting(User::getProviderId).containsExactly("kakao-2");
        assertThat(active)
                .extracting(User::getProviderId)
                .containsExactlyInAnyOrder("kakao-1", "naver-1");
    }

    @Test
    void provider로_필터링한다() {
        List<User> result =
                userRepository.findAll(AdminUserSpecifications.filter(null, null, "kakao"));

        assertThat(result)
                .extracting(User::getProviderId)
                .containsExactlyInAnyOrder("kakao-1", "kakao-2");
    }

    @Test
    void 필터를_조합할_수_있다() {
        List<User> result =
                userRepository.findAll(AdminUserSpecifications.filter(null, "ACTIVE", "kakao"));

        assertThat(result).extracting(User::getProviderId).containsExactly("kakao-1");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<User> result =
                userRepository.findAll(AdminUserSpecifications.filter(null, null, null));

        assertThat(result).hasSize(3);
    }
}
