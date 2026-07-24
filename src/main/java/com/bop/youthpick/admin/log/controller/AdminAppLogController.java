package com.bop.youthpick.admin.log.controller;

import com.bop.youthpick.admin.log.dto.ApplicationLogResponse;
import com.bop.youthpick.admin.log.service.AdminAppLogService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 애플리케이션 로그")
@RestController
@RequestMapping("/api/v1/admin/application-logs")
@RequiredArgsConstructor
@Validated
public class AdminAppLogController {

    private final AdminAppLogService adminAppLogService;

    @Operation(
            summary = "애플리케이션 로그 목록 조회",
            description = "로그 레벨, 키워드, 기간으로 애플리케이션 로그를 검색해 페이지 단위로 조회합니다.")
    @GetMapping
    public ApiResponse<List<ApplicationLogResponse>> list(
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "ERROR|WARN|INFO|DEBUG|^$",
                            message = "logLevel은 ERROR, WARN, INFO, DEBUG 중 하나여야 합니다.")
                    String logLevel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ApplicationLogResponse> page =
                adminAppLogService.search(logLevel, keyword, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }
}
