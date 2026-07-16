package com.bop.youthpick.admin.sync.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.sync.service.PolicySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/batch/policy-sync")
@RequiredArgsConstructor
public class AdminPolicySyncController {

    private final PolicySyncService policySyncService;

    /**
     * 정책 수집을 비동기로 시작시키고 즉시 202를 반환한다. 이미 실행 중이면 {@code SY001} 409 (GlobalExceptionHandler 변환). 결과
     * 확인은 {@code GET /api/v1/admin/batch-job-logs}(#57)로 한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> trigger() {
        policySyncService.startFullSyncAsync();
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }
}
