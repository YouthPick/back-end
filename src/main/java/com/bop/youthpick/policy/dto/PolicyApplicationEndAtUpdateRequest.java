package com.bop.youthpick.policy.dto;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record PolicyApplicationEndAtUpdateRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt) {}
