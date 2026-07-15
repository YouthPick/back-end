package com.bop.youthpick.user.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.dto.LoginHistoryResponse;
import com.bop.youthpick.user.service.AdminLoginHistoryService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/login-histories")
@RequiredArgsConstructor
public class AdminLoginHistoryController {

    private final AdminLoginHistoryService adminLoginHistoryService;

    @GetMapping
    public ApiResponse<List<LoginHistoryResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LoginHistoryResponse> page =
                adminLoginHistoryService.search(userId, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }
}
