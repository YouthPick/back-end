package com.bop.youthpick.admin.sync.controller;

import com.bop.youthpick.admin.sync.dto.BatchJobLogResponse;
import com.bop.youthpick.admin.sync.service.AdminBatchJobLogService;
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

@Tag(name = "관리자 - 배치 작업 이력")
@RestController
@RequestMapping("/api/v1/admin/batch-job-logs")
@RequiredArgsConstructor
@Validated
public class AdminBatchJobLogController {

    private final AdminBatchJobLogService adminBatchJobLogService;

    @Operation(summary = "배치 작업 이력 조회", description = "상태, 기간으로 배치 작업 이력 목록을 검색하여 페이지 단위로 조회한다.")
    @GetMapping
    public ApiResponse<List<BatchJobLogResponse>> list(
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "REQUESTED|RUNNING|SUCCEEDED|FAILED",
                            message = "status는 REQUESTED, RUNNING, SUCCEEDED, FAILED 중 하나여야 합니다.")
                    String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BatchJobLogResponse> page =
                adminBatchJobLogService.search(status, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }
}
