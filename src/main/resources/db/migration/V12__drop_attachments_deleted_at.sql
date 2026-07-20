-- V12 — attachments.deleted_at 제거
-- 첨부파일은 게시글 수정 시 최신 목록으로 물리 교체하며, 개별 첨부의 soft delete를 사용하지 않는다.
ALTER TABLE attachments
    DROP COLUMN deleted_at;
