-- ============================================================
-- V11 — user_profiles 온보딩 항목 확장 (#26)
--   취업상태/학력을 선택값에서 필수값으로 바꾸고, 결혼여부/전공/특화조건/연소득 컬럼을 추가한다.
--   major/income은 policies.major_codes/income_max_amount와 형식·단위를 통일해 맞춤정책 매칭에 쓴다.
-- ============================================================

ALTER TABLE user_profiles
    MODIFY COLUMN employment_status VARCHAR(16) NOT NULL COMMENT 'jobCd 1개 (REC 취업 15점)',
    MODIFY COLUMN education_level VARCHAR(16) NOT NULL COMMENT 'schoolCd 1개 (REC 학력 10점)',
    ADD COLUMN merry_status VARCHAR(16) NULL COMMENT '결혼여부 코드 (선택)' AFTER education_level,
    ADD COLUMN major VARCHAR(255) NULL COMMENT '전공 콤마목록, plcyMajorCd와 형식 통일 (선택)' AFTER merry_status,
    ADD COLUMN special_condition VARCHAR(500) NULL COMMENT '특화조건 콤마목록 (선택)' AFTER major,
    ADD COLUMN income INT NULL COMMENT '연소득(만원), policies.income_max_amount와 단위 통일 (선택)' AFTER special_condition;
