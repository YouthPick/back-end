package com.bop.youthpick.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    @Test
    void provider와_providerId가_일치하는_사용자를_찾는다() {
        userRepository.save(User.createSocialUser("GOOGLE", "google-1", "a@a.com", "닉네임"));

        Optional<User> found = userRepository.findByProviderAndProviderId("GOOGLE", "google-1");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("a@a.com");
    }

    @Test
    void 일치하는_사용자가_없으면_빈_값을_반환한다() {
        Optional<User> found = userRepository.findByProviderAndProviderId("GOOGLE", "unknown");

        assertThat(found).isEmpty();
    }
}
