package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.bop.youthpick.sync.exception.PolicySyncException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * PolicyApiClient 목 서버 테스트 — 실측 기반 실패 3종(성공/403 errorCode JSON/HTML)과 재시도·건수 검증. 실제 API를 치지 않는다.
 */
class PolicyApiClientTest {

    private static final String URL =
            "https://api.test/go/ythip/getPlcy"
                    + "?apiKeyNm={key}&rtnType=json&pageType=1&pageNum={page}&pageSize=1000";

    private MockRestServiceServer server;
    private PolicyApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PolicyApiClient(builder, "https://api.test", "test-key");
    }

    private static String pageJson(int totCount, String... plcyNos) {
        StringBuilder items = new StringBuilder();
        for (String no : plcyNos) {
            if (!items.isEmpty()) {
                items.append(',');
            }
            items.append("{\"plcyNo\":\"").append(no).append("\"}");
        }
        return """
                {"resultCode":200,"resultMessage":"OK",
                 "result":{"pagging":{"totCount":%d,"pageNum":1,"pageSize":1000},
                           "youthPolicyList":[%s]}}"""
                .formatted(totCount, items);
    }

    @Test
    @DisplayName("A: totCount까지 페이지를 순회해 전량 수집한다 (2페이지)")
    void fetchesAllPagesUntilTotCount() {
        // pageSize=1000이지만 테스트 응답은 페이지당 2건씩 잘라 페이지 순회를 검증한다
        server.expect(requestToUriTemplate(URL, "test-key", 1))
                .andExpect(queryParam("rtnType", "json"))
                .andExpect(queryParam("pageType", "1"))
                .andRespond(withSuccess(pageJson(3, "P1", "P2"), MediaType.APPLICATION_JSON));
        server.expect(requestToUriTemplate(URL, "test-key", 2))
                .andRespond(withSuccess(pageJson(3, "P3"), MediaType.APPLICATION_JSON));

        List<YouthPolicyItem> all = client.fetchAll();

        assertThat(all).extracting(YouthPolicyItem::plcyNo).containsExactly("P1", "P2", "P3");
        server.verify();
    }

    @Test
    @DisplayName("E: 403 + errorCode JSON(잘못된 키)은 3회 시도 후 회차 중단 예외")
    void failsAfterRetriesOn403() {
        server.expect(times(3), requestToUriTemplate(URL, "test-key", 1))
                .andRespond(
                        withStatus(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        "{\"errorCode\":\"e001\",\"errorMsg\":\"invalid api key.\"}"));

        assertThatThrownBy(client::fetchAll)
                .isInstanceOf(PolicySyncException.class)
                .hasMessageContaining("3회 모두 실패");
        server.verify();
    }

    @Test
    @DisplayName("E: HTML 응답(키 누락)은 JSON 파싱 실패로 간주하고 3회 시도 후 예외")
    void failsAfterRetriesOnHtmlResponse() {
        server.expect(times(3), requestToUriTemplate(URL, "test-key", 1))
                .andRespond(withSuccess("<html><body>error</body></html>", MediaType.TEXT_HTML));

        assertThatThrownBy(client::fetchAll)
                .isInstanceOf(PolicySyncException.class)
                .hasMessageContaining("3회 모두 실패");
        server.verify();
    }

    @Test
    @DisplayName("X: HTTP 200이라도 resultCode!=200이면 실패로 간주한다 (3중 체크)")
    void failsOnNonSuccessResultCode() {
        server.expect(times(3), requestToUriTemplate(URL, "test-key", 1))
                .andRespond(
                        withSuccess(
                                "{\"resultCode\":500,\"resultMessage\":\"server error\"}",
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::fetchAll).isInstanceOf(PolicySyncException.class);
        server.verify();
    }

    @Test
    @DisplayName("A: 일시 오류(5xx)는 재시도해서 성공하면 정상 수집한다")
    void retriesTransientErrorThenSucceeds() {
        server.expect(requestToUriTemplate(URL, "test-key", 1)).andRespond(withServerError());
        server.expect(requestToUriTemplate(URL, "test-key", 1))
                .andRespond(withSuccess(pageJson(1, "P1"), MediaType.APPLICATION_JSON));

        List<YouthPolicyItem> all = client.fetchAll();

        assertThat(all).extracting(YouthPolicyItem::plcyNo).containsExactly("P1");
        server.verify();
    }

    @Test
    @DisplayName("X: API 키 미설정이면 요청 없이 즉시 예외 (HTML 오류로 헤매는 사고 방지)")
    void failsFastWithoutApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer noCallServer = MockRestServiceServer.bindTo(builder).build();
        PolicyApiClient keylessClient = new PolicyApiClient(builder, "https://api.test", "");

        assertThatThrownBy(keylessClient::fetchAll)
                .isInstanceOf(PolicySyncException.class)
                .hasMessageContaining("YOUTH_API_KEY");
        noCallServer.verify();
    }
}
