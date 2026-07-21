-- ============================================================
-- V13 — user_profiles 취업/학력 컬럼 주석 정정
--   V1/V11은 이 두 컬럼에 온통청년 jobCd/schoolCd가 그대로 들어간다고 적었지만,
--   실제 온보딩(front-end profileOptions.ts)은 'UNEMPLOYED'/'UNIV_GRADUATE' 같은
--   서비스 자체 코드를 저장한다. 잘못된 주석 때문에 맞춤정책 매칭이 두 코드 체계를
--   그대로 비교해 취업·학력 축이 한 번도 일치하지 않는 버그가 있었다.
--   변환은 YouthPolicyCodeMapper가 담당한다(적용 완료된 V11은 checksum 때문에 수정하지 않고 여기서 덮어쓴다).
-- ============================================================

ALTER TABLE user_profiles
    MODIFY COLUMN employment_status VARCHAR(16) NOT NULL
        COMMENT '온보딩 취업상태 코드(UNEMPLOYED 등). jobCd 변환은 YouthPolicyCodeMapper (REC 취업 15점)',
    MODIFY COLUMN education_level VARCHAR(16) NOT NULL
        COMMENT '온보딩 학력 코드(UNIV_GRADUATE 등). schoolCd 변환은 YouthPolicyCodeMapper (REC 학력 10점)';
