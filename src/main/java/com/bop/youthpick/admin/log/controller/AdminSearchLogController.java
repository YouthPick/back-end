package com.bop.youthpick.admin.log.controller;

import com.bop.youthpick.admin.log.dto.SearchLogResponse;
import com.bop.youthpick.admin.log.service.AdminSearchLogService;
import com.bop.youthpick.global.common.ApiResponse;
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
@RequestMapping("/api/v1/admin/search-logs")
@RequiredArgsConstructor
public class AdminSearchLogController {

    private final AdminSearchLogService adminSearchLogService;

    @GetMapping
    public ApiResponse<List<SearchLogResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SearchLogResponse> page =
                adminSearchLogService.search(keyword, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }
}
