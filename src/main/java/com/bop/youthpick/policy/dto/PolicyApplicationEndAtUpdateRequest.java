package com.bop.youthpick.policy.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

public record PolicyApplicationEndAtUpdateRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endAt
) {}
