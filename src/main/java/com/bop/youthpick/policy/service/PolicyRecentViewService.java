package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.PolicyRecentViewResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRecentView;
import com.bop.youthpick.policy.repository.PolicyRecentViewRepository;
import com.bop.youthpick.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyRecentViewService {

    private final PolicyRecentViewRepository policyRecentViewRepository;
    private final UserRepository userRepository;

    /**
     * 정책 상세 조회 기록. 이미 본 정책이면 viewed_at만 갱신한다. 기록 실패가 상세 조회 응답까지 실패시키지 않도록 별도 트랜잭션(REQUIRES_NEW)으로
     * 실행한다 — 같은 트랜잭션에서 예외를 잡기만 하면 rollback-only로 표시돼 조회 커밋까지 함께 실패한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, Policy policy) {
        policyRecentViewRepository
                .findByUserIdAndPolicyId(userId, policy.getId())
                .ifPresentOrElse(
                        PolicyRecentView::touch,
                        () ->
                                policyRecentViewRepository.save(
                                        PolicyRecentView.create(
                                                userRepository.getReferenceById(userId), policy)));
    }

    @Transactional(readOnly = true)
    public Page<PolicyRecentViewResponse> getRecentPolicies(Long userId, Pageable pageable) {
        return policyRecentViewRepository
                .findVisibleByUserId(userId, pageable)
                .map(PolicyRecentViewResponse::from);
    }
}
