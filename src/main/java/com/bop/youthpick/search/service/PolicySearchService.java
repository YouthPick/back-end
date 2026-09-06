package com.bop.youthpick.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.search.config.PolicySearchProperties;
import com.bop.youthpick.search.dto.PolicyFacets;
import com.bop.youthpick.search.dto.PolicySearchQuery;
import com.bop.youthpick.search.dto.PolicySearchResult;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 정책 검색 질의. 색인에서 <b>조건에 맞는 정책 id</b>만 뽑아 온다.
 *
 * <p>여기서 나는 예외는 삼키지 않고 그대로 올린다 — 폴백 여부는 호출부({@code PolicyService})가 판단하고, 이 클래스는 검색만 책임진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySearchService {

    /** 검색어가 걸릴 필드와 가중치. 제목 일치를 본문 일치보다 크게 본다 — LIKE 에는 없던 개념이다. */
    private static final List<String> SEARCH_FIELDS =
            List.of(
                    "title^3",
                    "keywords^2",
                    "organizationName^2",
                    "description",
                    "supportContent",
                    // 지역명으로도 검색되게 한다(#199). keyword 필드는 정확 일치라 형태소 분석된 하위 필드를 쓴다.
                    "sidoNames.text");

    /** 검색어가 지역명과 맞을 때 그 지역 전용 정책에 주는 가산점. 실측으로 고른 값이다. */
    private static final float REGION_SPECIFIC_BOOST = 5.0f;

    /** filter 집계 안에 들어가는 terms 집계 이름. 두 패싯이 같은 모양이라 이름도 공유한다. */
    private static final String VALUES = "values";

    /** 표준 5분류. 늘어날 것을 대비해 여유를 뒀다. */
    private static final int CATEGORY_BUCKETS = 10;

    /** 시도 17개. 기본값 10을 그대로 두면 7개가 말없이 잘린다. */
    private static final int REGION_BUCKETS = 20;

    private final ElasticsearchClient client;
    private final PolicySearchProperties properties;

    /** 현재 색인된 문서 수. 인덱스가 아직 없거나 ES 가 죽어 있으면 0 을 돌려준다(재색인 판단용). */
    public long count() {
        try {
            return client.count(c -> c.index(properties.alias())).count();
        } catch (Exception e) {
            log.warn("색인 건수 조회 실패 — 0건으로 간주", e);
            return 0;
        }
    }

    public PolicySearchResult search(PolicySearchQuery query) throws Exception {
        BoolQuery bool = bool(query, true);
        SearchResponse<Void> response =
                client.search(
                        request ->
                                request.index(properties.alias())
                                        .query(q -> q.bool(bool))
                                        // 카드 내용은 MySQL 에서 읽으므로 _source 는 받지 않는다(전송량 절감).
                                        .source(s -> s.fetch(false))
                                        .from(query.page() * query.size())
                                        .size(query.size())
                                        // 기본값은 10,000건에서 집계를 멈춘다 — 페이징에 쓸 정확한 총건수가 필요하다.
                                        .trackTotalHits(t -> t.enabled(true))
                                        .sort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                                        // 점수가 같으면(검색어 없는 목록 조회 등) 최신순으로 안정 정렬한다.
                                        .sort(
                                                s ->
                                                        s.field(
                                                                f ->
                                                                        f.field("policyId")
                                                                                .order(
                                                                                        SortOrder
                                                                                                .Desc))),
                        Void.class);

        List<Long> policyIds =
                response.hits().hits().stream().map(hit -> Long.valueOf(hit.id())).toList();
        long total = response.hits().total() == null ? 0 : response.hits().total().value();
        return new PolicySearchResult(policyIds, total);
    }

    /**
     * 필터 UI 에 붙일 카테고리·지역별 건수. 목록 조회와 <b>같은 조건</b>에서 세되, 각 항목은 자기 자신의 필터만 뺀다.
     *
     * <p>구현은 "기준 질의 + 패싯별 filter 집계"다. 기준 질의에서 카테고리·지역 필터를 빼 두고, 카테고리 건수를 셀 때만 지역 필터를 다시 씌우고
     * 지역 건수를 셀 때만 카테고리 필터를 다시 씌운다. 이렇게 해야 "주거를 고른 상태에서도 다른 분류의 건수가 보인다".
     *
     * <p>hit 은 필요 없으므로 {@code size(0)} 이다 — 문서를 한 건도 실어 나르지 않고 숫자만 받는다.
     */
    public PolicyFacets facets(PolicySearchQuery query) throws Exception {
        BoolQuery base = bool(query, false);
        Query categoryFilter = query.category() == null ? matchAll() : categoryFilter(query.category());
        Query regionFilter = query.sidoName() == null ? matchAll() : regionFilter(query.sidoName());

        SearchResponse<Void> response =
                client.search(
                        request ->
                                request.index(properties.alias())
                                        .size(0)
                                        .query(q -> q.bool(base))
                                        .aggregations(
                                                "categories",
                                                a ->
                                                        a.filter(regionFilter)
                                                                .aggregations(
                                                                        VALUES,
                                                                        sub ->
                                                                                sub.terms(
                                                                                        t ->
                                                                                                t.field(
                                                                                                                "category")
                                                                                                        .size(
                                                                                                                CATEGORY_BUCKETS))))
                                        .aggregations(
                                                "regions",
                                                a ->
                                                        a.filter(categoryFilter)
                                                                .aggregations(
                                                                        VALUES,
                                                                        sub ->
                                                                                sub.terms(
                                                                                        t ->
                                                                                                t.field(
                                                                                                                "sidoNames")
                                                                                                        .size(
                                                                                                                REGION_BUCKETS)))),
                        Void.class);

        return new PolicyFacets(buckets(response, "categories"), buckets(response, "regions"));
    }

    /**
     * filter 집계 안의 terms 결과를 꺼낸다. terms 집계는 <b>기본 상위 10개만</b> 돌려주고 나머지는 말없이 버리므로 버킷 수를 명시했다 —
     * 시도는 17개라 기본값이면 7개가 조용히 사라진다.
     */
    private static List<PolicyFacets.FacetCount> buckets(SearchResponse<Void> response, String name) {
        return response.aggregations().get(name).filter().aggregations().get(VALUES).sterms()
                .buckets().array().stream()
                .map(b -> new PolicyFacets.FacetCount(b.key().stringValue(), b.docCount()))
                .toList();
    }

    /**
     * 질의 조립. filter 절은 점수에 영향을 주지 않고 걸러내기만 하고, should 절은 걸러내지 않고 점수만 올린다 — MySQL 이
     * {@code case when ... then 1 else 0} 정렬로 "후순위"를 표현했던 것을 ES 에서는 "가산점"으로 뒤집어 표현한다.
     *
     * @param withDrilldown 카테고리·지역 필터를 포함할지. 패싯 집계는 이 둘을 뺀 기준 집합에서 시작한다.
     */
    private BoolQuery bool(PolicySearchQuery query, boolean withDrilldown) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // ── 노출 조건 ── 삭제된 정책은 애초에 색인되지 않으므로 조건이 없다.
        bool.filter(f -> f.term(t -> t.field("visibility").value(PolicyVisibility.VISIBLE.name())));
        bool.filter(f -> f.term(t -> t.field("adminHidden").value(false)));

        // ── 마감 조건 ── 신청 마감일이 없으면(상시) 통과, 있으면 오늘 이후여야 한다.
        bool.filter(anyMatch(missing("applicationEndDate"), onOrAfter("applicationEndDate", query.today())));
        // 신청 마감일이 아예 없는 정책은 사업기간 종료일로 한 번 더 거른다(#123).
        bool.filter(
                anyMatch(
                        exists("applicationEndDate"),
                        missing("businessPeriodEnd"),
                        onOrAfter("businessPeriodEnd", query.today())));

        if (withDrilldown && query.category() != null) {
            bool.filter(categoryFilter(query.category()));
        }
        if (withDrilldown && query.sidoName() != null) {
            bool.filter(regionFilter(query.sidoName()));
            // 전국 정책은 모든 시도에 걸려 있어 지역 검색에 항상 잡힌다. 지역 특화 정책을 위로 올린다.
            bool.should(s -> s.term(t -> t.field("nationwide").value(false)));
        }
        // ── 나이 ── 요청 구간과 정책 자격 구간의 겹침. 값이 없거나 0이면 '제한 없음'이라 통과시킨다.
        if (query.ageMin() != null) {
            bool.filter(
                    anyMatch(
                            missing("maxAge"),
                            isZero("maxAge"),
                            atLeast("maxAge", query.ageMin())));
        }
        if (query.ageMax() != null) {
            bool.filter(
                    anyMatch(missing("minAge"), isZero("minAge"), atMost("minAge", query.ageMax())));
        }
        if (query.jobCode() != null) {
            bool.filter(
                    anyMatch(
                            missing("jobCodes"),
                            anyOf(
                                    "jobCodes",
                                    List.of(query.jobCode(), Policy.JOB_CODE_UNRESTRICTED))));
            // '제한없음'으로 걸린 정책보다 해당 코드를 실제로 가진 정책을 위로 올린다.
            bool.should(s -> s.term(t -> t.field("jobCodes").value(query.jobCode())));
        }

        if (query.keyword() != null) {
            bool.must(
                    m ->
                            m.multiMatch(
                                    mm ->
                                            mm.query(query.keyword())
                                                    .fields(SEARCH_FIELDS)
                                                    // cross_fields: 여러 필드를 한 덩어리처럼 본다.
                                                    // best_fields 는 "한 필드 안에서" 조건을 세기 때문에,
                                                    // "서울 청년 취업"처럼 지역(sidoNames)과 제목에 단어가
                                                    // 나뉘어 있으면 어떤 필드도 전부 갖지 못해 걸러졌다.
                                                    .type(TextQueryType.CrossFields)
                                                    // 단어가 3개 이상이면 70% 이상 맞아야 한다. 기본값(OR)은
                                                    // '청년'처럼 흔한 한 단어만 맞아도 전부 걸려 필터 구실을 못 한다.
                                                    .minimumShouldMatch("2<70%")));
            // 검색어에 지역명이 들어 있으면 그 지역 전용 정책을 전국 정책보다 위로 올린다(#199 후속).
            // 전국 정책은 모든 시도에 지역 행이 걸려 있어 지역명 검색에 항상 잡히는데, 이 규칙이 없으면
            // "서울 청년 취업" 상위가 전부 전국 정책으로 채워진다.
            // 가산점 5는 실측으로 정했다 — 없으면 전국이 상위를 독점하고, 15면 여러 시도를 걸친 정책이
            // 해당 지역 전용 정책보다 앞선다(es/helper 로 상위 결과를 비교).
            bool.should(
                    s ->
                            s.bool(
                                    b ->
                                            b.must(
                                                            m ->
                                                                    m.match(
                                                                            mt ->
                                                                                    mt.field(
                                                                                                    "sidoNames.text")
                                                                                            .query(
                                                                                                    query
                                                                                                            .keyword())))
                                                    .must(
                                                            m ->
                                                                    m.term(
                                                                            t ->
                                                                                    t.field(
                                                                                                    "nationwide")
                                                                                            .value(
                                                                                                    false)))
                                                    .boost(REGION_SPECIFIC_BOOST)));
        }
        return bool.build();
    }

    /** 목록 조회의 filter 절과 패싯 집계의 filter 가 같은 조건이어야 해서 따로 뽑았다. */
    private static Query categoryFilter(String category) {
        return Query.of(q -> q.term(t -> t.field("category").value(category)));
    }

    /** MySQL 의 EXISTS 서브쿼리가 배열 필드 term 하나로 줄었다. */
    private static Query regionFilter(String sidoName) {
        return Query.of(q -> q.term(t -> t.field("sidoNames").value(sidoName)));
    }

    private static Query matchAll() {
        return Query.of(q -> q.matchAll(m -> m));
    }

    /** 여러 조건 중 하나만 맞으면 통과하는 filter 절(SQL 의 OR). */
    private static Query anyMatch(Query... alternatives) {
        return Query.of(
                q -> q.bool(b -> b.should(List.of(alternatives)).minimumShouldMatch("1")));
    }

    private static Query missing(String field) {
        return Query.of(q -> q.bool(b -> b.mustNot(mn -> mn.exists(e -> e.field(field)))));
    }

    private static Query exists(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    private static Query isZero(String field) {
        return Query.of(q -> q.term(t -> t.field(field).value(0)));
    }

    private static Query onOrAfter(String field, LocalDate date) {
        return Query.of(q -> q.range(r -> r.date(d -> d.field(field).gte(date.toString()))));
    }

    private static Query atLeast(String field, int value) {
        return Query.of(q -> q.range(r -> r.number(n -> n.field(field).gte((double) value))));
    }

    private static Query atMost(String field, int value) {
        return Query.of(q -> q.range(r -> r.number(n -> n.field(field).lte((double) value))));
    }

    private static Query anyOf(String field, List<String> values) {
        List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
        return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fieldValues))));
    }
}
