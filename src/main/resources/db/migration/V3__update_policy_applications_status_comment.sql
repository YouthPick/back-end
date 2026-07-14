-- ============================================================
-- V3 — policy_applications.status 컬럼 코멘트 갱신
--   ApplicationStatus에 PREPARING(준비중) 추가 (2026-07-14) 후
--   V1의 코멘트가 예전 3단계 값만 나열해 실제 허용값과 어긋나 있었음.
--   CHECK 제약은 없으므로(문서용 코멘트) 데이터 마이그레이션은 불필요.
-- ============================================================

ALTER TABLE policy_applications
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'INTERESTED'
        COMMENT 'INTERESTED(관심=구 즐겨찾기) | PREPARING(준비중) | APPLIED | COMPLETED';
