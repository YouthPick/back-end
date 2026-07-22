-- 신청기간·URL 데이터 정합성 백필 (#123)
-- 신규 수집분은 sync PolicyMapper(parseApplyPeriod/normalizeUrl)가 같은 규칙으로 저장 전에 정리한다.
-- 이 마이그레이션은 그 규칙을 이미 적재된 기존 행에도 동일하게 적용한다.

-- 1) 신청기간(aplyYmd)이 비어 applicationEndDate가 null인 정책 중, 진짜 상시(0057002)가 아니면서
--    사업기간(businessPeriodEnd)은 있는 경우 사업기간으로 신청기간을 대체한다.
--    (0057003류 지역 단발성 모집이 신청마감일 없음="상시"로 계속 노출되던 문제)
UPDATE policies
SET application_start_date = COALESCE(application_start_date, business_period_begin),
    application_end_date = business_period_end
WHERE application_period_type != '0057002'
  AND application_end_date IS NULL
  AND business_period_end IS NOT NULL;

-- 2) 스킴 없는 도메인형 URL("www.xxx.go.kr")에 https:// 를 붙인다
UPDATE policies SET application_url = CONCAT('https://', application_url)
WHERE application_url IS NOT NULL AND application_url NOT LIKE 'http%'
  AND application_url LIKE '%.%' AND application_url NOT LIKE '% %';

UPDATE policies SET reference_url1 = CONCAT('https://', reference_url1)
WHERE reference_url1 IS NOT NULL AND reference_url1 NOT LIKE 'http%'
  AND reference_url1 LIKE '%.%' AND reference_url1 NOT LIKE '% %';

UPDATE policies SET reference_url2 = CONCAT('https://', reference_url2)
WHERE reference_url2 IS NOT NULL AND reference_url2 NOT LIKE 'http%'
  AND reference_url2 LIKE '%.%' AND reference_url2 NOT LIKE '% %';

-- 3) URL이 아닌 자유 텍스트("-", "전화문의" 등)는 깨진 링크를 만들지 않도록 null로 정리한다
UPDATE policies SET application_url = NULL
WHERE application_url IS NOT NULL AND application_url <> '' AND application_url NOT LIKE 'http%';

UPDATE policies SET reference_url1 = NULL
WHERE reference_url1 IS NOT NULL AND reference_url1 <> '' AND reference_url1 NOT LIKE 'http%';

UPDATE policies SET reference_url2 = NULL
WHERE reference_url2 IS NOT NULL AND reference_url2 <> '' AND reference_url2 NOT LIKE 'http%';
