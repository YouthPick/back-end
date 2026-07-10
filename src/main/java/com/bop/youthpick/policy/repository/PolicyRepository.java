package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {}
