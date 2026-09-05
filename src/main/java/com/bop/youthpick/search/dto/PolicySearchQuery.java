package com.bop.youthpick.search.dto;

import java.time.LocalDate;
import org.springframework.lang.Nullable;

/**
 * 정책 검색 조건. {@code PolicyService.getCards} 의 파라미터를 그대로 옮긴 것이며, 기존 LIKE 경로와 같은 결과 집합을 내는 것이
 * 목표다(정렬은 관련도 점수가 추가로 개입한다).
 *
 * @param keyword 검색어. null 이면 조건 없이 필터만 적용한다
 * @param category 표준 5분류 정확 일치
 * @param sidoName 시도명 정확 일치. '전국' 선택은 호출부에서 null 로 바꿔 넘긴다
 * @param ageMin 요청 나이 구간 하한 (정책 자격 구간과 겹치는지 판정)
 * @param ageMax 요청 나이 구간 상한
 * @param jobCode 온통청년 취업상태 코드. 해당 코드 정책과 '제한없음' 정책을 함께 통과시킨다
 * @param today 마감 판정 기준일. 호출 시점을 넘겨 테스트에서 고정할 수 있게 한다
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기
 */
public record PolicySearchQuery(
        @Nullable String keyword,
        @Nullable String category,
        @Nullable String sidoName,
        @Nullable Integer ageMin,
        @Nullable Integer ageMax,
        @Nullable String jobCode,
        LocalDate today,
        int page,
        int size) {}
