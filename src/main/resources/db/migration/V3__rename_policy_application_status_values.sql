-- ============================================================
-- V3 — policy_applications.status 값 체계를 프론트엔드 계약에 맞춰 변경
--   기존: INTERESTED | APPLIED | COMPLETED
--   변경: INTERESTED | PREPARING | SUBMITTED | CLOSED (PREPARING 신규 중간 상태 추가)
-- ============================================================

UPDATE policy_applications SET status = 'SUBMITTED' WHERE status = 'APPLIED';
UPDATE policy_applications SET status = 'CLOSED' WHERE status = 'COMPLETED';

ALTER TABLE policy_applications
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'INTERESTED'
        COMMENT 'INTERESTED(관심=구 즐겨찾기) | PREPARING(준비중) | SUBMITTED(신청완료) | CLOSED(종료)';
