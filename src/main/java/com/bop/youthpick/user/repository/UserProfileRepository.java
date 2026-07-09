package com.bop.youthpick.user.repository;

import com.bop.youthpick.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);
}
