package com.bop.youthpick.policy.dto;

/**
 * 반환할 데이터가 없는 체크리스트 액션(check/uncheck/delete)의 응답 껍데기 — PolicyApplicationChecklistController가 문자열
 * 리터럴("체크 완료" 등)을 직접 담아 만든다. 값을 만드는 서비스 로직은 없고, 순전히 컨트롤러가 사람이 읽을 성공 메시지를 응답 body에 넣기 위한 래퍼일 뿐이다.
 */
public record PolicyApplicationChecklistMessageResponse(String message) {}
