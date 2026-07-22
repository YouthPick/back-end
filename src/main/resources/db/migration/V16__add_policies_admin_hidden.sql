ALTER TABLE policies
    ADD COLUMN admin_hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- 기존 HIDDEN은 관리자 화면에서 수동으로 숨긴 데이터와 배치 누락 데이터를 구분할 이력이 없다.
-- 기존 관리자 숨김이 다음 배치에서 다시 공개되지 않도록 보수적으로 보존한다.
UPDATE policies
SET admin_hidden = TRUE
WHERE visibility = 'HIDDEN';
