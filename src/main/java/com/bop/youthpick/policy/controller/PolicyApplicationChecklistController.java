package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistMessageResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationCreateChecklistRequest;
import com.bop.youthpick.policy.dto.PolicyApplicationUpdateChecklistRequest;
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

/**
 * 정책 신청관리(PolicyApplication) 하위의 준비 체크리스트(제출서류 등) HTTP 진입점. 모든 엔드포인트는 결국
 * PolicyApplicationChecklistService의 findActiveApplication()/findActive()를 거쳐, 대상 체크리스트가 (1)삭제되지
 * 않았고 (2)그 부모 {@link com.bop.youthpick.policy.entity.PolicyApplication}도 삭제되지 않았고 (3)요청자(userId)
 * 소유인지를 확인한 뒤에만 동작한다 — 이 컨트롤러 자체엔 소유권/존재 검증이 없다.
 *
 * <p>이 클래스엔 {@code @Validated}가 없다 — PolicyApplicationController와 달리 여기 모든 검증은
 * {@code @Valid @RequestBody} DTO(Create/UpdateChecklistRequest)로만 이루어지고, {@code @RequestParam}에 직접
 * 붙는 제약(예: {@code @Pattern})이 없기 때문이다({@code @Validated}는 그 경우에만 필요하다).
 */
@RestController
@RequestMapping("/api/v1/policy-application-checklists")
@RequiredArgsConstructor
public class PolicyApplicationChecklistController {

    private final PolicyApplicationChecklistService checklistService;

    /**
     * {@code POST /api/v1/policy-application-checklists} — 특정 신청관리(applicationId)에 체크리스트 항목 추가.
     * {@code request.message()}가 {@link PolicyApplicationChecklistService#add}의 message 인자로 그대로
     * 넘어가고, 서비스 내부에서 {@code PolicyApplicationChecklist.create()}가 이걸 content 필드에 담는다 — DTO는
     * "message", 엔티티/리포지토리는 "content"로 이름이 다르니 헷갈리지 않도록 주의.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationChecklistResponse>> add(
            @CurrentUser Long userId,
            @Valid @RequestBody PolicyApplicationCreateChecklistRequest request) {
        PolicyApplicationChecklist checklist =
                checklistService.add(request.applicationId(), userId, request.message());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationChecklistResponse.from(checklist)));
    }

    /**
     * {@code GET /api/v1/policy-application-checklists/application/{applicationId}} — 특정 신청관리에 달린
     * 체크리스트 목록. 서비스가 먼저 부모 신청관리의 존재/소유권을 확인한 뒤(쿼리 1회), 체크리스트 목록을 id 오름차순(등록 순서)으로 조회한다(쿼리 1회) — 총
     * 2쿼리.
     */
    @GetMapping("/application/{applicationId}")
    public ApiResponse<List<PolicyApplicationChecklistResponse>> getByApplication(
            @CurrentUser Long userId,
            @PathVariable Long applicationId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyApplicationChecklistResponse> page =
                checklistService.getByApplication(applicationId, userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    /** {@code PATCH /api/v1/policy-application-checklists/{id}} — 체크리스트 내용(content) 수정. */
    @PatchMapping("/{id}")
    public ApiResponse<PolicyApplicationChecklistResponse> update(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PolicyApplicationUpdateChecklistRequest request) {
        PolicyApplicationChecklist checklist =
                checklistService.update(id, userId, request.message());
        return ApiResponse.ok(PolicyApplicationChecklistResponse.from(checklist));
    }

    /**
     * {@code PATCH /api/v1/policy-application-checklists/{id}/check} — 체크 표시. body 없이 상태만 토글하므로 반환
     * 데이터가 없고, {@link PolicyApplicationChecklistMessageResponse}로 성공 메시지만 내려준다({@link
     * PolicyApplicationController#delete}가 {@code ApiResponse.ok(null)}로 데이터 없음을 표현하는 것과 다른 스타일 —
     * 여긴 메시지 body를 쓴다).
     */
    @PatchMapping("/{id}/check")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> check(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.check(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 완료"));
    }

    /** {@code PATCH /api/v1/policy-application-checklists/{id}/uncheck} — {@link #check}의 반대. */
    @PatchMapping("/{id}/uncheck")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> uncheck(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.uncheck(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크 해제 완료"));
    }

    /**
     * {@code DELETE /api/v1/policy-application-checklists/{id}} — 체크리스트 항목 하나만 소프트 삭제. 부모 신청관리 전체가
     * 삭제/재등록될 때 체크리스트를 한 번에 정리하는 경로는 여기가 아니라 {@code
     * PolicyApplicationChecklistRepository.softDeleteAllByApplicationId()}이고, 그건 이 컨트롤러가 아니라 {@code
     * PolicyApplicationService.create()}의 reactivate 분기에서 호출된다(다른 도메인 흐름과의 연결점).
     */
    @DeleteMapping("/{id}")
    public ApiResponse<PolicyApplicationChecklistMessageResponse> delete(
            @CurrentUser Long userId, @PathVariable Long id) {
        checklistService.delete(id, userId);
        return ApiResponse.ok(new PolicyApplicationChecklistMessageResponse("체크리스트 삭제 완료"));
    }
}
