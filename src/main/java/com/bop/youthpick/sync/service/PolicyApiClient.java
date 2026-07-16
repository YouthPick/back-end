package com.bop.youthpick.sync.service;

import com.bop.youthpick.sync.dto.YouthPolicyApiResponse;
import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.bop.youthpick.sync.exception.PolicySyncException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 온통청년 정책 API 클라이언트 [Reader]. pageSize=1000 페이지네이션으로 전량 수집하고 누적 수신 == totCount를 검증한다.
 *
 * <p>성공 3중 체크: ① HTTP 200 (4xx/5xx는 RestClient가 예외) → ② JSON 파싱 (HTML 응답은 여기서 실패) → ③
 * resultCode==200 & youthPolicyList 존재. 페이지당 {@value MAX_ATTEMPTS}회 시도 후 실패 시 PolicySyncException으로
 * 회차 전체를 중단시킨다 (배치 설계 §5 — fetch 실패 시 DB 손 안 댐).
 *
 * <p>타임아웃은 spring.http.client.* 설정이 자동구성 RestClient.Builder에 적용한다 (application.yml).
 */
@Component
public class PolicyApiClient {

    private static final Logger log = LoggerFactory.getLogger(PolicyApiClient.class);

    private static final int PAGE_SIZE = 1000;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final String apiKey;

    public PolicyApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${youthpick.sync.base-url:https://www.youthcenter.go.kr}") String baseUrl,
            @Value("${youthpick.sync.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /** 전량 수집. 누적 수신 건수가 totCount와 다르면 예외 (누락된 채 비교/HIDDEN 처리하는 사고 방지). */
    public List<YouthPolicyItem> fetchAll() {
        if (apiKey.isBlank()) {
            throw new PolicySyncException("온통청년 API 키가 없습니다 — 환경변수 YOUTH_API_KEY를 설정하세요");
        }
        List<YouthPolicyItem> all = new ArrayList<>();
        int totCount;
        int pageNum = 1;
        do {
            YouthPolicyApiResponse.Result result = fetchPageWithRetry(pageNum++);
            totCount = result.paging().totCount();
            if (result.youthPolicyList().isEmpty() && all.size() < totCount) {
                throw new PolicySyncException(
                        "빈 페이지 수신 — 누적 %d건, totCount %d건 (API 응답 이상)"
                                .formatted(all.size(), totCount));
            }
            all.addAll(result.youthPolicyList());
        } while (all.size() < totCount);

        if (all.size() != totCount) {
            throw new PolicySyncException(
                    "수신 건수 불일치 — 누적 %d건 != totCount %d건".formatted(all.size(), totCount));
        }
        log.info("정책 전량 수집 완료 — {}건 ({}페이지)", all.size(), pageNum - 1);
        return all;
    }

    private YouthPolicyApiResponse.Result fetchPageWithRetry(int pageNum) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return requestPage(pageNum);
            } catch (RestClientException | PolicySyncException e) {
                // 키 값은 로그 금지 — 예외 메시지에도 URL 전체를 싣지 않는다
                log.warn(
                        "정책 API {}페이지 요청 실패 ({}/{}): {}",
                        pageNum,
                        attempt,
                        MAX_ATTEMPTS,
                        e.getMessage());
                lastFailure = e;
            }
        }
        throw new PolicySyncException(
                "정책 API %d페이지 요청이 %d회 모두 실패 — 회차 중단".formatted(pageNum, MAX_ATTEMPTS), lastFailure);
    }

    private YouthPolicyApiResponse.Result requestPage(int pageNum) {
        YouthPolicyApiResponse response =
                restClient
                        .get()
                        .uri(
                                uri ->
                                        uri.path("/go/ythip/getPlcy")
                                                .queryParam("apiKeyNm", apiKey)
                                                .queryParam("rtnType", "json")
                                                .queryParam("pageType", "1")
                                                .queryParam("pageNum", pageNum)
                                                .queryParam("pageSize", PAGE_SIZE)
                                                .build())
                        .retrieve()
                        .body(YouthPolicyApiResponse.class);
        if (response == null
                || response.resultCode() == null
                || response.resultCode() != 200
                || response.result() == null
                || response.result().paging() == null
                || response.result().paging().totCount() == null
                || response.result().youthPolicyList() == null) {
            throw new PolicySyncException(
                    "정책 API 응답이 성공 형식이 아닙니다 (resultCode="
                            + (response == null ? "null" : response.resultCode())
                            + ")");
        }
        return response.result();
    }
}
