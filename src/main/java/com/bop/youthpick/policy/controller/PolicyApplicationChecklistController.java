package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.ApplicationChecklistResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistMessageResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationCreateChecklistRequest;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
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

// TODO: 인증 도입 후 @PathVariable managementId/id 대신 인증 principal 기반 소유권 검증으로 교체한다.
@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class PolicyApplicationChecklistController {

    private final PolicyApplicationChecklistService checklistService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationChecklistResponse>> add(
            @Valid @RequestBody PolicyApplicationCreateChecklistRequest request) {
        ApplicationChecklist checklist =
                checklistService.add(request.managementId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ApplicationChecklistResponse.from(checklist)));
    }

    @GetMapping("/management/{managementId}")
    public ApiResponse<List<ApplicationChecklistResponse>> getByManagement(
            @PathVariable Long managementId, @PageableDefault(size = 20) Pageable pageable) {
        Page<ApplicationChecklistResponse> page =
                checklistService.getByManagement(managementId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @PatchMapping("/{id}/check")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> check(@PathVariable Long id) {
        checklistService.check(id);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 완료"));
    }

    @PatchMapping("/{id}/uncheck")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> uncheck(@PathVariable Long id) {
        checklistService.uncheck(id);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 해제 완료"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> delete(@PathVariable Long id) {
        checklistService.delete(id);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크리스트 삭제 완료"));
    }
}
