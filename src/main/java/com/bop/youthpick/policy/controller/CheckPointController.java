package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.ApplicationChecklistResponse;
import com.bop.youthpick.policy.dto.CheckPointMessageResponse;
import com.bop.youthpick.policy.dto.CreateCheckPointRequest;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.bop.youthpick.policy.service.CheckPointService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkpoints")
@RequiredArgsConstructor
public class CheckPointController {

    private final CheckPointService checkPointService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationChecklistResponse>> add(
            @Valid @RequestBody CreateCheckPointRequest request) {
        ApplicationChecklist checklist =
                checkPointService.add(request.managementId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ApplicationChecklistResponse.from(checklist)));
    }

    @GetMapping("/management/{managementId}")
    public ApiResponse<List<ApplicationChecklistResponse>> getByManagement(
            @PathVariable Long managementId) {
        return ApiResponse.ok(checkPointService.getByManagement(managementId));
    }

    @PatchMapping("/{id}/check")
    public ApiResponse<CheckPointMessageResponse> check(@PathVariable Long id) {
        checkPointService.check(id);
        return ApiResponse.ok(new CheckPointMessageResponse("체크 완료"));
    }

    @PatchMapping("/{id}/uncheck")
    public ApiResponse<CheckPointMessageResponse> uncheck(@PathVariable Long id) {
        checkPointService.uncheck(id);
        return ApiResponse.ok(new CheckPointMessageResponse("체크 해제 완료"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<CheckPointMessageResponse> delete(@PathVariable Long id) {
        checkPointService.delete(id);
        return ApiResponse.ok(new CheckPointMessageResponse("체크리스트 삭제 완료"));
    }
}
