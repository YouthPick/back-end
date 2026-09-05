package com.bop.youthpick.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bop.youthpick.search.config.PolicySearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 정책 검색. 지금은 색인 건수 조회만 있고, 실제 검색 질의는 7단계에서 채운다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySearchService {

    private final ElasticsearchClient client;
    private final PolicySearchProperties properties;

    /** 현재 색인된 문서 수. 인덱스가 아직 없거나 ES가 죽어 있으면 0을 돌려준다(재색인 판단용). */
    public long count() {
        try {
            return client.count(c -> c.index(properties.alias())).count();
        } catch (Exception e) {
            log.warn("색인 건수 조회 실패 — 0건으로 간주", e);
            return 0;
        }
    }
}
