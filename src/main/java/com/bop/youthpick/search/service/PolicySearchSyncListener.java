package com.bop.youthpick.search.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.search.dto.PolicyChangedEvent;
import com.bop.youthpick.search.dto.PolicyDocument;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 정책 변경을 검색 색인에 반영한다. 색인이 MySQL 을 따라가게 만드는 유일한 지점이다.
 *
 * <p>{@link TransactionPhase#AFTER_COMMIT} 으로 받는 이유: 커밋 전에 색인하면 이후 롤백됐을 때 MySQL 에는 없는 내용이
 * ES 에만 남는다. MySQL 이 원본이고 ES 는 사본이므로 원본이 확정된 뒤에만 사본을 고친다.
 *
 * <p>여기서 나는 예외는 전부 삼킨다. 트랜잭션은 이미 끝나서 되돌릴 수 없고, 어긋난 문서는 다음 전체 재색인 때 메워진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicySearchSyncListener {

    private final PolicyRepository policyRepository;
    private final PolicyDocumentAssembler assembler;
    private final PolicySearchIndexer indexer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PolicyChangedEvent event) {
        try {
            if (event.removed()) {
                event.policyIds().forEach(indexer::delete);
                return;
            }
            List<Policy> policies = policyRepository.findAllById(event.policyIds());
            List<PolicyDocument> documents = assembler.assemble(policies);
            indexer.index(documents);
            // 삭제됐거나 사라진 정책은 문서가 만들어지지 않는다 — 색인에 남아 있으면 지운다.
            deleteMissing(event.policyIds(), documents);
        } catch (Exception e) {
            log.warn("정책 색인 반영 실패 {}건 — 다음 전체 재색인 때 메워진다", event.policyIds().size(), e);
        }
    }

    private void deleteMissing(List<Long> requestedIds, List<PolicyDocument> documents) {
        Set<Long> indexed =
                documents.stream().map(PolicyDocument::policyId).collect(Collectors.toSet());
        requestedIds.stream().filter(id -> !indexed.contains(id)).forEach(indexer::delete);
    }
}
