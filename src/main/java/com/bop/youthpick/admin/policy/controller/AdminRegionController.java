package com.bop.youthpick.admin.policy.controller;

import com.bop.youthpick.admin.policy.service.AdminRegionService;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 지역")
@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
public class AdminRegionController {

    private final AdminRegionService adminRegionService;

    @Operation(summary = "지역 목록 조회", description = "전체 지역 목록을 조회한다.")
    @GetMapping
    public ApiResponse<List<RegionResponse>> list() {
        return ApiResponse.ok(adminRegionService.findAll());
    }
}
