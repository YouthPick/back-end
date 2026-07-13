-- ============================================================
-- YouthPick ERD — ERDCloud 정리본 (2026-07-07)
-- 기준: docs/2026-07-03-youthpick-erd.sql + ERDCloud 추가 테이블
--
-- ERDCloud 대비 변경:
--   삭제: CopyOfuser_profiles, CopyOfCopyOfCopyOffavorite_policies,
--         CopyOfpolicy_region(→ regions로 복구), board, Untitled
--   삭제(설계 결정 2026-07-07): favorite_policies —
--         정책 신청관리(policy_applications)의 status='INTERESTED'가
--         즐겨찾기 역할을 흡수. 별도 테이블 불필요
--   복원: policies 영어 물리명, regions, policy_batch_history
--   정리: 신청관리·게시판·로그 테이블 컬럼명/타입/FK
--   soft delete: 유저 소유 데이터 전부 deleted_at 보유
--         (로그·배치이력은 의도적으로 없음 — 이력은 삭제하지 않음,
--          policies는 visibility가 그 역할)
--   감사 컬럼: 유저 도메인 전 테이블 created_at+updated_at 통일
--         (BaseEntity 상속과 1:1 대응. 예외 = 불변 데이터:
--          regions/policy_regions 없음, 로그는 created_at만,
--          batch_history는 자체 시각)
--   ⚠️팀 결정 필요 표시는 [결정필요]로 검색
-- ============================================================

-- ------------------------------------------------------------
-- 1. users — 서비스 회원 (소셜 로그인 전용)
--    [결정필요] ERDCloud에서 email·nickname이 user_profiles로
--    이동돼 있었음. 온보딩 전 사용자도 표시명이 필요하므로
--    원본대로 users에 유지 (중복 배치 금지)
-- ------------------------------------------------------------
CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    provider     VARCHAR(20)  NOT NULL COMMENT 'google | naver | kakao',
    provider_id  VARCHAR(255) NOT NULL COMMENT 'OAuth subject',
    email        VARCHAR(255) NULL     COMMENT 'provider 미제공 시 NULL',
    nickname     VARCHAR(100) NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   DATETIME     NULL     COMMENT '회원탈퇴 soft delete',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_provider (provider, provider_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '서비스 회원';

-- ------------------------------------------------------------
-- 2. regions — 지역 마스터
--    ERDCloud의 CopyOfpolicy_region이 이 테이블의 잔해였음.
--    코드는 VARCHAR(10) — BIGINT면 앞자리 0 소실 + FK 불가
-- ------------------------------------------------------------
CREATE TABLE regions (
    code       VARCHAR(10) NOT NULL COMMENT '시군구 코드 5자리 (앞 2자리 = 시도)',
    sido_name  VARCHAR(30) NOT NULL COMMENT '시도명',
    name       VARCHAR(50) NOT NULL COMMENT '시군구명',
    PRIMARY KEY (code)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '지역 마스터 (행안부 법정동코드 시군구 ~250행)';

-- ------------------------------------------------------------
-- 3. user_profiles — 온보딩 프로필 (users와 1:1)
--    ERDCloud의 id2 → user_id로 정정, deleted_at 복원
-- ------------------------------------------------------------
CREATE TABLE user_profiles (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NOT NULL,
    birth_year         INT          NOT NULL COMMENT '출생연도 (REC 나이 20점)',
    region_code        VARCHAR(10)  NOT NULL COMMENT '거주 시군구코드 (REC 지역 25점)',
    employment_status  VARCHAR(16)  NULL     COMMENT 'jobCd 1개 (REC 취업 15점)',
    education_level    VARCHAR(16)  NULL     COMMENT 'schoolCd 1개 (REC 학력 10점)',
    categories         VARCHAR(500) NULL     COMMENT '관심분야 콤마목록 (REC 분야 20점)',
    keywords           VARCHAR(700) NULL     COMMENT '관심키워드 콤마목록 (REC 키워드 10점)',
    status             VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         DATETIME     NULL     COMMENT '프로필 삭제 soft delete',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_profiles_user (user_id),
    CONSTRAINT fk_user_profiles_user   FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_user_profiles_region FOREIGN KEY (region_code) REFERENCES regions (code)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '온보딩 프로필 (점수계산 입력값)';

-- ------------------------------------------------------------
-- 4. policies — 청년정책 (물리명 = 영어, 논리명/설명 = COMMENT)
--    ERDCloud에서 설명문이 물리명 자리에 들어가 있던 것을 복원
-- ------------------------------------------------------------
CREATE TABLE policies (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    policy_no                   VARCHAR(32)   NOT NULL COMMENT 'plcyNo — 온통청년 식별자 (upsert 기준)',
    -- 표시/검색
    title                       VARCHAR(300)  NOT NULL COMMENT 'plcyNm — 정책명',
    description                 VARCHAR(2000) NULL COMMENT 'plcyExplnCn — 목록 설명',
    support_content             TEXT          NULL COMMENT 'plcySprtCn — 상세 지원내용',
    keywords                    VARCHAR(500)  NULL COMMENT 'plcyKywdNm — 키워드 콤마목록',
    category                    VARCHAR(64)   NULL COMMENT 'lclsfNm — 대분류',
    middle_category             VARCHAR(64)   NULL COMMENT 'mclsfNm — 중분류',
    organization_name           VARCHAR(255)  NULL COMMENT 'sprvsnInstCdNm 우선, operInstCdNm fallback',
    -- 자격: 자동판정용
    min_age                     INT           NULL COMMENT 'sprtTrgtMinAge — 0/NULL=제한없음',
    max_age                     INT           NULL COMMENT 'sprtTrgtMaxAge',
    job_codes                   VARCHAR(255)  NULL COMMENT 'jobCd — 콤마 다중',
    school_codes                VARCHAR(255)  NULL COMMENT 'schoolCd — 콤마 다중',
    -- 자격: 원문 노출용
    income_condition_code       VARCHAR(16)   NULL COMMENT 'earnCndSeCd',
    income_max_amount           INT           NULL COMMENT 'earnMaxAmt — 연소득 상한(만원)',
    income_etc_content          TEXT          NULL COMMENT 'earnEtcCn',
    marital_status_code         VARCHAR(16)   NULL COMMENT 'mrgSttsCd',
    major_codes                 VARCHAR(255)  NULL COMMENT 'plcyMajorCd — 콤마 다중',
    specialization_codes        VARCHAR(255)  NULL COMMENT 'sbizCd — 콤마 다중',
    additional_qualification    TEXT          NULL COMMENT 'addAplyQlfcCndCn',
    participation_restriction   TEXT          NULL COMMENT 'ptcpPrpTrgtCn',
    -- 기간/상태
    application_period_type     VARCHAR(16)   NULL COMMENT 'aplyPrdSeCd',
    application_period_raw      VARCHAR(64)   NULL COMMENT 'aplyYmd 원문',
    application_start_date      DATE          NULL COMMENT '[파생] 신청 시작일',
    application_end_date        DATE          NULL COMMENT '[파생] 마감일 — D-day 정렬/필터',
    business_period_begin       DATE          NULL COMMENT 'bizPrdBgngYmd',
    business_period_end         DATE          NULL COMMENT 'bizPrdEndYmd',
    business_period_etc         VARCHAR(64)   NULL COMMENT 'bizPrdEtcCn',
    -- 규모
    support_scale_count         INT           NULL COMMENT 'sprtSclCnt — 모집인원',
    first_come_first_served     BOOLEAN       NOT NULL DEFAULT FALSE COMMENT 'sprtArvlSeqYn — 선착순',
    -- 링크/신청
    application_url             VARCHAR(500)  NULL COMMENT 'aplyUrlAddr',
    reference_url1              VARCHAR(500)  NULL COMMENT 'refUrlAddr1',
    reference_url2              VARCHAR(500)  NULL COMMENT 'refUrlAddr2',
    application_method          TEXT          NULL COMMENT 'plcyAplyMthdCn',
    submission_documents        TEXT          NULL COMMENT 'sbmsnDcmntCn',
    screening_method            TEXT          NULL COMMENT 'srngMthdCn',
    -- 보류 필드 (적재만, 판정 사용 금지)
    age_limit_flag              VARCHAR(4)    NULL COMMENT '[보류] sprtTrgtAgeLmtYn',
    income_min_amount           INT           NULL COMMENT '[보류] earnMinAmt',
    support_scale_limit         BOOLEAN       NULL COMMENT '[보류] sprtSclLmtYn',
    operating_institution_name  VARCHAR(255)  NULL COMMENT '[보류] operInstCdNm',
    approval_status_code        VARCHAR(16)   NULL COMMENT '[보류] plcyAprvSttsCd',
    etc_matters                 TEXT          NULL COMMENT '[보류] etcMttrCn',
    -- 메타/정렬/운영
    view_count                  INT           NOT NULL DEFAULT 0 COMMENT 'inqCnt — 인기 정렬',
    first_registered_at         DATETIME      NULL COMMENT 'frstRegDt — 최신순 정렬',
    last_modified_at            DATETIME      NULL COMMENT 'lastMdfcnDt — 변경 감지',
    visibility                  VARCHAR(20)   NOT NULL DEFAULT 'VISIBLE' COMMENT 'VISIBLE | HIDDEN',
    raw_payload                 LONGTEXT      NULL COMMENT 'API 응답 원문 JSON',
    created_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_policies_policy_no (policy_no),
    KEY idx_policies_application_end (application_end_date),
    KEY idx_policies_view_count (view_count),
    KEY idx_policies_category (category)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '청년정책 (온통청년 수집·전처리 결과)';

-- ------------------------------------------------------------
-- 5. policy_regions — 정책 적용지역 (N:M)
--    ERDCloud의 `지역pk` BIGINT → region_code VARCHAR(10) 정정
-- ------------------------------------------------------------
CREATE TABLE policy_regions (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    policy_id   BIGINT      NOT NULL,
    region_code VARCHAR(10) NOT NULL COMMENT 'zipCd 분해',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_regions (policy_id, region_code),
    KEY idx_policy_regions_region (region_code),
    CONSTRAINT fk_policy_regions_policy FOREIGN KEY (policy_id)   REFERENCES policies (id),
    CONSTRAINT fk_policy_regions_region FOREIGN KEY (region_code) REFERENCES regions (code)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '정책 적용지역';

-- ------------------------------------------------------------
-- 6. policy_applications — 정책 신청관리 (ERDCloud `정책 신청관리`)
--    ID/id/id2 3개 → id + user_id + policy_id로 정리.
--    기존 favorite_policies 흡수: status='INTERESTED'가 즐겨찾기.
--    UNIQUE(user_id, policy_id) — 같은 정책 중복 등록 방지
--    (즐겨찾기 시절 UNIQUE의 계승)
-- ------------------------------------------------------------
CREATE TABLE policy_applications (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    policy_id  BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'INTERESTED' COMMENT 'INTERESTED(관심=구 즐겨찾기) | APPLIED | COMPLETED',
    memo       TEXT        NULL,
    end_at     DATETIME    NULL COMMENT '마감일 (개인 설정)',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '상태 전환(관심→신청→완료) 추적',
    deleted_at DATETIME    NULL COMMENT '관리 해제 = soft delete. 재등록 시 행 재활성화(UNIQUE 충돌 방지)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_applications_user_policy (user_id, policy_id),
    CONSTRAINT fk_policy_applications_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_policy_applications_policy FOREIGN KEY (policy_id) REFERENCES policies (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '사용자별 정책 신청 진행관리 (관심~완료 전 단계, 즐겨찾기 통합)';

-- ------------------------------------------------------------
-- 7. policy_application_checklists — 신청관리 체크리스트
--    p_id → application_id, status TINYINT → is_checked
-- ------------------------------------------------------------
CREATE TABLE policy_application_checklists (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    application_id BIGINT   NOT NULL COMMENT 'policy_applications FK',
    content        TEXT     NOT NULL COMMENT '체크 항목 내용',
    is_checked     BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     DATETIME NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_policy_application_checklists_app (application_id),
    CONSTRAINT fk_policy_application_checklists_app FOREIGN KEY (application_id) REFERENCES policy_applications (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '신청관리별 준비 체크리스트';

-- ------------------------------------------------------------
-- 8. posts — 게시판 (ERDCloud `게시글`)
--    `Key` VARCHAR → policy_id BIGINT NULL(자유글 허용)로 정정
-- ------------------------------------------------------------
CREATE TABLE posts (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL COMMENT '글쓴이',
    policy_id  BIGINT       NULL COMMENT '연관 정책 (NULL = 자유글)',
    category   VARCHAR(20)  NOT NULL COMMENT 'QUESTION(질문) | REVIEW(후기) | FREE(자유) — ENUM 대신 VARCHAR(role과 동일 이유)',
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    view_count INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_posts_policy (policy_id),
    CONSTRAINT fk_posts_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_posts_policy FOREIGN KEY (policy_id) REFERENCES policies (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '정책 기반 게시판 글';

-- ------------------------------------------------------------
-- 9. comments — 댓글 (ERDCloud `댓글`)
--     Key → parent_id(대댓글), Field3/4/Field → created/updated/deleted_at
-- ------------------------------------------------------------
CREATE TABLE comments (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    post_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    parent_id  BIGINT   NULL COMMENT '대댓글 부모 (NULL = 최상위)',
    content    TEXT     NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_comments_post (post_id),
    CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)   REFERENCES posts (id),
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '게시글 댓글 (1단 대댓글)';

-- ------------------------------------------------------------
-- 10. attachments — 첨부파일 (ERDCloud `첨부파일`)
--     Key/Key2/Field/Field2/Field3 → 의미 있는 이름으로
--     [미정] 첨부 대상이 게시글인지 아직 미확정 — 일단 post_id로
--     두고, 대상 바뀌면 FK만 교체
-- ------------------------------------------------------------
CREATE TABLE attachments (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    file_url   VARCHAR(500) NOT NULL COMMENT '저장 경로/URL',
    file_size  BIGINT       NULL COMMENT 'byte',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_attachments_post (post_id),
    CONSTRAINT fk_attachments_post FOREIGN KEY (post_id) REFERENCES posts (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '게시글 첨부파일';

-- ------------------------------------------------------------
-- 11. policy_batch_history — 배치 작업 이력 (ERDCloud 누락 → 복원)
-- ------------------------------------------------------------
CREATE TABLE policy_batch_history (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    mode            VARCHAR(20)   NOT NULL COMMENT 'FULL | DELTA',
    status          VARCHAR(20)   NOT NULL COMMENT 'REQUESTED | RUNNING | SUCCEEDED | FAILED',
    requested_at    DATETIME      NOT NULL,
    started_at      DATETIME      NULL,
    finished_at     DATETIME      NULL,
    new_count       INT           NOT NULL DEFAULT 0,
    updated_count   INT           NOT NULL DEFAULT 0,
    unchanged_count INT           NOT NULL DEFAULT 0,
    missing_count   INT           NOT NULL DEFAULT 0,
    error_count     INT           NOT NULL DEFAULT 0,
    failure_message VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_policy_batch_history_requested (requested_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '정책 수집 배치 이력 (FK 없음 — 독립 이력)';

-- ------------------------------------------------------------
-- 12. application_logs — 앱 에러/요청 로그 (ERDCloud `로그`)
--     VARCHAR 길이 누락 정정, user_id NULL 허용(비로그인/배치),
--     FK 없음 — 로그는 유저 삭제와 무관하게 보존
--     V3 마이그레이션에서 app_logs → application_logs로 개명(ERD 물리명 기준)
-- ------------------------------------------------------------
CREATE TABLE application_logs (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    level             VARCHAR(10)  NOT NULL COMMENT 'INFO | WARN | ERROR',
    message           TEXT         NOT NULL,
    trace_id          VARCHAR(64)  NULL,
    method            VARCHAR(10)  NULL COMMENT 'GET | POST ...',
    uri               TEXT         NULL,
    ip                VARCHAR(45)  NULL COMMENT 'IPv6 최대 45자',
    exception_class   VARCHAR(255) NULL,
    exception_message TEXT         NULL,
    stack_trace       TEXT         NULL,
    user_id           BIGINT       NULL COMMENT '비로그인/배치 = NULL. FK 안 걺',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_application_logs_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '앱 로그 (기한 지나면 삭제)';

-- ------------------------------------------------------------
-- 13. search_histories — 검색 로그 (ERDCloud `검색용 로그`)
--     개수 VARCHAR → INT, user_id NULL 허용, 물리명 영어로
-- ------------------------------------------------------------
CREATE TABLE search_histories (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NULL COMMENT '비로그인 NULL. FK 안 걺',
    query        VARCHAR(200) NOT NULL COMMENT '입력 원문',
    normalized   VARCHAR(200) NOT NULL COMMENT '소문자·공백정리 — 집계용',
    result_count INT          NOT NULL COMMENT '검색 결과 건수 (0 = 0건 검색)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_search_histories_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '검색 로그 (집계 후 원본 정리)';

-- ============================================================
-- [결정필요] 원본에 있었으나 이 정리본에 미포함:
--   policy_read_states — 읽음 상태 (원본에서도 "팀 논의 중").
--   포함하려면 docs/2026-07-03-youthpick-erd.sql §6 참조.
-- ============================================================
