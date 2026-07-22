package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.service.RegionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 지역 마스터 전체 목록 조회 (비회원 허용). 온보딩 프로필의 거주지역 선택지로 쓰인다. */
@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public ApiResponse<List<RegionResponse>> list() {
        return ApiResponse.ok(regionService.getAllRegions());
    }
}
