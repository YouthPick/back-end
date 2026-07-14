package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdatePolicyApplicationRequest(
        @Size(max = 500, message = "메모는 500자를 초과할 수 없습니다.") String memo, LocalDateTime endAt) {}
