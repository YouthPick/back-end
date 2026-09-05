package com.bop.youthpick.search.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.search.dto.PolicyDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * MySQL의 전체 정책을 Elasticsearch에 처음부터 다시 색인한다.
 *
 * <p>배치({@code PolicyUpsertWriter})의 증분 색인만으로는 이미 저장돼 있던 정책이 영영 들어가지 않는다. 초기 1회 적재와 매핑 변경
 * 후 재색인이 이 클래스의 역할이다.
 *
 * <p>인덱스가 비어 있을 때만 동작하므로 켜 둔 채로 재기동해도 안전하다 — {@code youthpick.sync.startup-on-empty}와 같은
 * 방식. alias를 새 인덱스(policy_v2)로 옮기면 그쪽이 비어 있으니 자동으로 다시 채워진다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "youthpick.search.reindex-on-empty", havingValue = "true")
@RequiredArgsConstructor
public class PolicyReindexer implements ApplicationRunner {

    /** 한 번에 MySQL에서 읽어올 정책 수. 색인 벌크(500)와 맞춰 페이지 하나가 벌크 하나가 되게 한다. */
    private static final int PAGE_SIZE = 500;

    private final PolicyRepository policyRepository;
    private final PolicyDocumentAssembler assembler;
    private final PolicySearchIndexer indexer;
    private final PolicySearchService searchService;

    @Override
    public void run(ApplicationArguments args) {
        long indexed = searchService.count();
        if (indexed > 0) {
            log.info("색인 {}건 존재 — 전체 재색인 생략", indexed);
            return;
        }
        reindexAll();
    }

    /** 전체 정책을 페이지 단위로 읽어 색인한다. 실패해도 예외를 던지지 않고 건수만 로그로 남긴다. */
    @Transactional(readOnly = true)
    public void reindexAll() {
        long started = System.currentTimeMillis();
        int total = 0;
        int failed = 0;

        for (int page = 0; ; page++) {
            List<Policy> policies =
                    policyRepository
                            .findAll(PageRequest.of(page, PAGE_SIZE, Sort.by("id")))
                            .getContent();
            if (policies.isEmpty()) {
                break;
            }
            List<PolicyDocument> documents = assembler.assemble(policies);
            failed += indexer.index(documents);
            total += documents.size();
            log.info("색인 진행 {}건", total);
        }

        log.info(
                "전체 재색인 완료 — {}건, 실패 {}건, {}ms",
                total,
                failed,
                System.currentTimeMillis() - started);
    }
}
