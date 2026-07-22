package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.Size;

public record PolicyApplicationMemoUpdateRequest(
        @Size(max = 2000, message = "메모는 2000자를 초과할 수 없습니다.") String memo) {}
