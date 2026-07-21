package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyChatMessageRepository extends JpaRepository<PolicyChatMessage, Long> {

    @EntityGraph(attributePaths = "user")
    List<PolicyChatMessage> findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
            Long policyId, Long afterId, Pageable pageable);
}
