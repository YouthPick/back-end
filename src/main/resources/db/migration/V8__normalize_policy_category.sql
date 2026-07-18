-- 정책 대분류(category) 정규화 (#80)
-- 원본(온통청년 lclsfNm)에 콤마 다중값('일자리,일자리'), 반각 가운뎃점(U+FF65 '･'), 구명칭('참여권리')이
-- 섞여 있어 표준 5분류(일자리 / 주거 / 교육·직업훈련 / 금융·복지·문화 / 참여·기반, 구분자 U+00B7)로 통일한다.
-- 신규 수집분은 sync PolicyMapper.normalizeCategory가 같은 규칙으로 저장 전에 정규화한다.

-- 1) 콤마 다중값은 첫 값만 남긴다
UPDATE policies
SET category = TRIM(SUBSTRING_INDEX(category, ',', 1))
WHERE category LIKE '%,%';

-- 2) 가운뎃점 변형(U+FF65 '･', U+30FB '・')을 표준 U+00B7('·')로 통일
UPDATE policies
SET category = REPLACE(REPLACE(category, '･', '·'), '・', '·')
WHERE category IS NOT NULL;

-- 3) 구명칭·무점 표기를 표준 명칭으로
UPDATE policies SET category = '교육·직업훈련' WHERE category IN ('교육', '교육직업훈련');
UPDATE policies SET category = '금융·복지·문화' WHERE category IN ('복지문화', '복지·문화', '금융복지문화');
UPDATE policies SET category = '참여·기반' WHERE category IN ('참여권리', '참여·권리', '참여기반');
