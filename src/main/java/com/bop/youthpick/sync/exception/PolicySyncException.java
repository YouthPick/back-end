package com.bop.youthpick.sync.exception;

/**
 * 배치 내부 실패 (fetch 실패, 건수 불일치 등) — 회차 전체 중단용. HTTP 응답으로 나가는 예외가 아니라 PolicySyncJob이 잡아 이력을 FAILED로
 * 기록한다. 관리자 API용 에러코드(409 등)는 Phase 5에서 별도 정의.
 */
public class PolicySyncException extends RuntimeException {

    public PolicySyncException(String message) {
        super(message);
    }

    public PolicySyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
