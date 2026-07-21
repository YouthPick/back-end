-- ============================================================
-- V13 — policies.is_nationwide 추가 (#120)
--   전 시도를 커버하는 정책 여부. 배치가 policy_regions와 함께 계산해 저장하는 파생 값이다.
--
--   용도: 지역 필터 조회 시 지역 특화 정책을 먼저 노출하고 전국 정책을 뒤로 보내는 정렬 키.
--   온통청년 원본 zipCd가 지역 한정 정책에도 전국 코드를 담고 있어(예: 서산시 정책에 251개 코드),
--   지역을 선택해도 전국 정책이 상단을 차지하는 문제가 있다. 목록에서 제외하면 해당 지역 청년이
--   신청 가능한 전국 정책을 못 보게 되므로, 숨기지 않고 순서만 뒤로 민다.
--
--   판정: 정책이 커버하는 시도 수 = regions의 전체 시도 수. 노출 대상 기준 단일 시도 1,331건 /
--   전 시도 241건 / 중간(2~15시도) 7건인 이봉분포라 임계값 없이 '전 시도'로 가른다.
--
--   ※ 인덱스는 만들지 않는다. 2값 저장 컬럼은 카디널리티가 낮아 옵티마이저가 거의 쓰지 않는다.
--     필요해지면 (visibility, is_nationwide) 복합으로 검토한다.
-- ============================================================

ALTER TABLE policies
    ADD COLUMN is_nationwide BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT '[파생] 전 시도 커버 여부 — 배치가 policy_regions 기준으로 계산'
        AFTER missing_count;

-- 기존 데이터 백필 — 배치 재실행 없이 즉시 정렬이 동작하도록 한다.
UPDATE policies p
SET p.is_nationwide = TRUE
WHERE (SELECT COUNT(DISTINCT r.sido_name)
         FROM policy_regions pr
         JOIN regions r ON r.code = pr.region_code
        WHERE pr.policy_id = p.id)
      >= (SELECT COUNT(DISTINCT sido_name) FROM regions);
