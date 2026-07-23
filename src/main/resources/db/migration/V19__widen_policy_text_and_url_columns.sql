-- 온통청년 원본이 컬럼 상한을 넘겨 정책 저장이 매 회차 실패하던 문제(#207).
-- MySQL은 MODIFY 시 COMMENT를 다시 지정하지 않으면 지우므로 원본 주석을 그대로 유지한다.

-- bizPrdEtcCn은 자유 서술 텍스트인데 V1에서 날짜 컬럼(bizPrdBgngYmd/bizPrdEndYmd) 옆에 묶이면서
-- 짧은 값으로 가정돼 VARCHAR(64)로 잡혔다. 같은 성격의 earnEtcCn/addAplyQlfcCndCn/ptcpPrpTrgtCn과
-- 동일하게 TEXT로 맞춘다.
ALTER TABLE policies
    MODIFY COLUMN business_period_etc TEXT NULL COMMENT 'bizPrdEtcCn';

-- 쿼리스트링이 붙은 원본 URL이 500자를 넘는 사례가 있다.
ALTER TABLE policies
    MODIFY COLUMN application_url VARCHAR(1000) NULL COMMENT 'aplyUrlAddr',
    MODIFY COLUMN reference_url1 VARCHAR(1000) NULL COMMENT 'refUrlAddr1',
    MODIFY COLUMN reference_url2 VARCHAR(1000) NULL COMMENT 'refUrlAddr2';

-- aplyYmd 원문은 신청기간이 여러 개면 '20260101 ~ 20261231\N20270101 ~ 20271231'처럼 늘어난다.
-- 2개만 돼도 43자라 기존 64자 상한은 3개째에서 터진다.
ALTER TABLE policies
    MODIFY COLUMN application_period_raw VARCHAR(255) NULL COMMENT 'aplyYmd 원문';
