package com.bop.youthpick.user.repository;

import com.bop.youthpick.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
