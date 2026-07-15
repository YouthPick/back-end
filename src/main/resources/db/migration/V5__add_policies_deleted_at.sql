-- ============================================================
-- V5 — policies.deleted_at 추가 (관리자 정책 삭제)
--   visibility(VISIBLE/HIDDEN)는 배치가 원본 소실 정책을 껐다 켜는
--   용도로 계속 쓰고, deleted_at은 관리자가 확정적으로 삭제 처리한
--   것을 별도로 표시하는 soft delete 컬럼이다. 두 상태는 독립적이다.
-- ============================================================

ALTER TABLE policies
    ADD COLUMN deleted_at DATETIME NULL COMMENT '관리자 soft delete 시각';
