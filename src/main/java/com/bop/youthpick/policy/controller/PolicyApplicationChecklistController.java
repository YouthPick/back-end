package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistMessageResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationCreateChecklistRequest;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import com.bop.youthpick.policy.service.PolicyApplicationChecklistService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class PolicyApplicationChecklistController {

    private final PolicyApplicationChecklistService checklistService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationChecklistResponse>> add(
            @CurrentUser Long userId,
            @Valid @RequestBody PolicyApplicationCreateChecklistRequest request) {
        PolicyApplicationChecklist checklist =
                checklistService.add(request.applicationId(), userId, request.message());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationChecklistResponse.from(checklist)));
    }

    @GetMapping("/application/{applicationId}")
    public ApiResponse<List<PolicyApplicationChecklistResponse>> getByApplication(
            @CurrentUser Long userId,
            @PathVariable Long applicationId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyApplicationChecklistResponse> page =
                checklistService.getByApplication(applicationId, userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @PatchMapping("/{id}/check")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> check(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.check(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 완료"));
    }

    @PatchMapping("/{id}/uncheck")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> uncheck(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.uncheck(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 해제 완료"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> delete(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.delete(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크리스트 삭제 완료"));
    }
}
