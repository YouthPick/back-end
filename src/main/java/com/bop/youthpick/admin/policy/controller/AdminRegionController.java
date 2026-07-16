package com.bop.youthpick.admin.policy.controller;

import com.bop.youthpick.admin.policy.service.AdminRegionService;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/regions")
@RequiredArgsConstructor
public class AdminRegionController {

    private final AdminRegionService adminRegionService;

    @GetMapping
    public ApiResponse<List<RegionResponse>> list() {
        return ApiResponse.ok(adminRegionService.findAll());
    }
}
