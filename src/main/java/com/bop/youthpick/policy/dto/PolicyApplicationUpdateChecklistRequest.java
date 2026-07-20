package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PolicyApplicationChecklistController.update()가 바인딩하는 요청 DTO. Create 요청과 달리 applicationId가 없다 — 어느
 * 신청관리 소속인지는 이미 URL의 {id}(체크리스트 자신의 PK)로 정해지고, PolicyApplicationChecklistService.findActive()가 그
 * id로 부모까지 함께 조회하기 때문이다. message는 PolicyApplicationChecklist.updateContent()로 그대로 전달된다.
 */
public record PolicyApplicationUpdateChecklistRequest(
        @NotBlank(message = "체크리스트 내용은 필수입니다.")
                @Size(max = 500, message = "체크리스트 내용은 500자 이하여야 합니다.")
                String message) {}
