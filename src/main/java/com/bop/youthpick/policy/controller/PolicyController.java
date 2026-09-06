package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyCardResponse;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.service.PolicyService;
import com.bop.youthpick.search.dto.PolicyFacets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책")
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /**
     * 정책 목록(카드) 조회 (비회원 허용). 기본 최신순이되, region·jobCode 필터가 걸리면 조건 없는 정책(전국/취업상태 제한없음)을 뒤로 민다.
     * category(표준 5분류) exact match, keyword는 부분일치 검색, region은 시도명('전국'은 지역 무관이라 필터 미적용),
     * ageMin/ageMax는 자격 구간과의 겹침, jobCode는 온통청년 취업상태 코드(예: 0013001 재직자) — 모두 선택. meta에
     * page/totalCount/totalPages.
     */
    @Operation(
            summary = "정책 목록 조회",
            description =
                    "정책 목록(카드) 조회(비회원 허용). 기본 최신순이되 region·jobCode 필터가 걸리면 조건 없는 정책(전국/취업상태 제한없음)을 뒤로 민다."
                            + " category(표준 5분류) exact match, keyword는 부분일치 검색, region은 시도명('전국'은 지역 무관이라 필터"
                            + " 미적용), ageMin/ageMax는 자격 구간과의 겹침, jobCode는 온통청년 취업상태 코드(예: 0013001 재직자) — 모두 선택."
                            + " meta에 page/totalCount/totalPages.")
    @GetMapping
    public ApiResponse<List<PolicyCardResponse>> getCards(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) String jobCode,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyCardResponse> page =
                policyService.getCards(
                        category, keyword, region, ageMin, ageMax, jobCode, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    /**
     * 필터 UI 에 붙일 카테고리·지역별 건수. 목록 조회와 같은 파라미터를 그대로 받는다(page/size 만 없다).
     *
     * <p>각 목록은 자기 자신의 필터를 뺀 조건에서 센 값이라, 카테고리를 고른 상태에서도 다른 분류의 건수가 보인다.
     */
    @Operation(
            summary = "정책 필터 건수 조회",
            description =
                    "필터 UI 에 붙일 카테고리·지역별 건수(비회원 허용). 목록 조회와 같은 파라미터를 받는다. 각 목록은 자기 자신의 필터를 뺀"
                            + " 조건에서 세므로 카테고리를 고른 상태에서도 다른 분류의 건수가 보인다. 빈 목록은 0건이 아니라 집계 불가(ES 미사용/장애)를"
                            + " 뜻한다.")
    @GetMapping("/facets")
    public ApiResponse<PolicyFacets> getFacets(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) String jobCode) {
        return ApiResponse.ok(
                policyService.getFacets(category, keyword, region, ageMin, ageMax, jobCode));
    }

    /** 정책 상세 조회 (비회원 허용). 로그인 사용자의 조회는 최근 본 정책으로 기록된다. */
    @Operation(summary = "정책 상세 조회", description = "정책 상세 조회(비회원 허용). 로그인 사용자의 조회는 최근 본 정책으로 기록된다.")
    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDetailResponse> getDetail(
            @PathVariable Long policyId, @CurrentUser(required = false) Long userId) {
        return ApiResponse.ok(policyService.getDetail(policyId, userId));
    }
}
