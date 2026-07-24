package com.bop.youthpick.sync.dto;

/** Writer 실행 집계 — policy_batch_history 카운트 기록용. */
public record PolicyWriteResult(int newCount, int updatedCount, int errorCount) {}
