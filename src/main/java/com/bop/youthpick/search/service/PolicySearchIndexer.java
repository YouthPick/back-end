package com.bop.youthpick.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.bop.youthpick.search.config.PolicySearchProperties;
import com.bop.youthpick.search.dto.PolicyDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 정책 문서를 Elasticsearch 에 색인한다.
 *
 * <p>색인 실패는 예외로 던지지 않고 로그 + 실패 건수로 돌려준다. 색인은 MySQL 저장이 끝난 뒤에 하는 부가 작업이라,
 * 여기서 터지면 배치 전체가 롤백되거나 동기화가 중단된다. ES 가 잠깐 죽어도 정책 수집은 계속돼야 하고, 빠진 문서는
 * 다음 전체 재색인 때 메워진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySearchIndexer {

    /**
     * 한 번에 보낼 문서 수. 배치의 {@code PolicyUpsertWriter.CHUNK_SIZE}(100)보다 크게 잡은 이유는 ES 색인이 DB 트랜잭션과
     * 달리 락을 잡지 않아서다. 너무 크면 요청 본문이 커져 ES 쪽 메모리를 밀어내므로, 문서 크기를 감안해 500 으로 둔다.
     */
    private static final int BULK_SIZE = 500;

    private final ElasticsearchClient client;
    private final PolicySearchProperties properties;

    /**
     * 문서들을 색인하고 <b>실패 건수</b>를 돌려준다.
     *
     * @return 색인하지 못한 문서 수 (0 이면 전부 성공)
     */
    public int index(List<PolicyDocument> documents) {
        if (!properties.enabled() || documents.isEmpty()) {
            return 0;
        }

        int failed = 0;
        for (int from = 0; from < documents.size(); from += BULK_SIZE) {
            int to = Math.min(from + BULK_SIZE, documents.size());
            failed += indexChunk(documents.subList(from, to));
        }
        return failed;
    }

    /**
     * 문서 1건을 색인에서 지운다. 관리자가 정책을 삭제(soft delete)했을 때 검색 결과에 계속 노출되는 것을 막는다.
     *
     * <p>이미 없는 문서를 지우려 해도 ES는 404를 예외로 올리지 않고 result=not_found 로 응답하므로 따로 처리하지 않는다.
     */
    public void delete(Long policyId) {
        if (!properties.enabled()) {
            return;
        }
        try {
            client.delete(d -> d.index(properties.alias()).id(String.valueOf(policyId)));
        } catch (Exception e) {
            log.warn("정책 색인 삭제 실패 id={}", policyId, e);
        }
    }

    private int indexChunk(List<PolicyDocument> chunk) {
        BulkRequest.Builder request = new BulkRequest.Builder();
        for (PolicyDocument document : chunk) {
            // index = "있으면 덮어쓰고 없으면 만든다". 문서 id 를 정책 PK 로 고정했으므로 재색인해도 중복되지 않는다.
            request.operations(op -> op.index(idx -> idx.index(properties.alias())
                .id(document.documentId())
                .document(document)));
        }

        try {
            BulkResponse response = client.bulk(request.build());
            if (!response.errors()) {
                return 0;
            }
            // bulk 는 일부만 실패해도 HTTP 200 이다. 항목별로 뒤져야 실패를 알 수 있다.
            int failed = 0;
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    failed++;
                    log.warn("정책 색인 실패 id={} reason={}", item.id(), item.error().reason());
                }
            }
            return failed;
        } catch (Exception e) {
            log.error("정책 색인 요청 실패 size={}", chunk.size(), e);
            return chunk.size();
        }
    }
}
