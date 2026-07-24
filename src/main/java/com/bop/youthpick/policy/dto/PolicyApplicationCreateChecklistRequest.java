package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PolicyApplicationChecklistController.add()가 바인딩하는 요청 DTO. applicationId는
 * PolicyApplicationChecklistService.findActiveApplication()이 PolicyApplicationRepository로 조회해
 * 존재/소유권을 확인하는 데 쓰이고, message는 PolicyApplicationChecklist.create()에서 content 필드에 담긴다 (create 쪽 500자
 * 제한은 PolicyApplicationCreateRequest의 메모 2000자 제한보다 짧다 — 체크리스트 항목 하나의 길이 제약이라 더 짧게 잡혀 있다).
 */
public record PolicyApplicationCreateChecklistRequest(
        @NotNull(message = "신청관리 ID는 필수입니다.") Long applicationId,
        @NotBlank(message = "체크리스트 내용은 필수입니다.")
                @Size(max = 500, message = "체크리스트 내용은 500자 이하여야 합니다.")
                String message) {}
