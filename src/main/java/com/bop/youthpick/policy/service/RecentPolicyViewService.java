package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.RecentPolicyResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.RecentPolicyView;
import com.bop.youthpick.policy.repository.RecentPolicyViewRepository;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecentPolicyViewService {

    /** 사용자당 최근 본 정책 보관 상한. 초과분은 오래된 것부터 삭제한다. */
    static final int MAX_RECENT_VIEWS = 50;

    private final RecentPolicyViewRepository recentPolicyViewRepository;
    private final UserRepository userRepository;

    /** 정책 상세 조회 기록. 이미 본 정책이면 viewed_at만 갱신하고, 보관 상한 초과분은 오래된 것부터 지운다. */
    @Transactional
    public void record(Long userId, Policy policy) {
        recentPolicyViewRepository
                .findByUserIdAndPolicyId(userId, policy.getId())
                .ifPresentOrElse(
                        RecentPolicyView::touch,
                        () ->
                                recentPolicyViewRepository.save(
                                        RecentPolicyView.create(
                                                userRepository.getReferenceById(userId), policy)));

        long count = recentPolicyViewRepository.countByUserId(userId);
        if (count > MAX_RECENT_VIEWS) {
            List<RecentPolicyView> overflow =
                    recentPolicyViewRepository.findByUserIdOrderByViewedAtAsc(
                            userId, PageRequest.of(0, (int) (count - MAX_RECENT_VIEWS)));
            recentPolicyViewRepository.deleteAll(overflow);
        }
    }

    @Transactional(readOnly = true)
    public Page<RecentPolicyResponse> getRecentPolicies(Long userId, Pageable pageable) {
        return recentPolicyViewRepository
                .findVisibleByUserId(userId, pageable)
                .map(RecentPolicyResponse::from);
    }
}
