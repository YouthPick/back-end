-- ============================================================
-- V1 — YouthPick 초기 스키마
-- 원본: docs/schema.sql (2026-07-07 ERD 정리본, PR #10)
-- 대상: MySQL 8.x 전용 (H2 local/test는 Flyway 비활성, ddl-auto 사용)
-- 이 파일은 적용 후 수정 금지 — 변경은 V2+ 마이그레이션으로 추가
-- ============================================================

-- ------------------------------------------------------------
-- 1. users — 서비스 회원 (소셜 로그인 전용)
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
-- ------------------------------------------------------------
CREATE TABLE regions (
    code       VARCHAR(10) NOT NULL COMMENT '시군구 코드 5자리 (앞 2자리 = 시도)',
    sido_name  VARCHAR(30) NOT NULL COMMENT '시도명',
    name       VARCHAR(50) NOT NULL COMMENT '시군구명',
    PRIMARY KEY (code)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '지역 마스터 (행안부 법정동코드 시군구 ~250행)';

-- ------------------------------------------------------------
-- 3. user_profiles — 온보딩 프로필 (users와 1:1)
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
-- 6. policy_applications — 정책 신청관리 (즐겨찾기 통합)
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
-- 8. posts — 게시판
-- ------------------------------------------------------------
CREATE TABLE posts (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL COMMENT '글쓴이',
    policy_id  BIGINT       NULL COMMENT '연관 정책 (NULL = 자유글)',
    category   VARCHAR(20)  NOT NULL COMMENT 'QUESTION(질문) | REVIEW(후기) | FREE(자유)',
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
-- 9. comments — 댓글 (1단 대댓글)
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
-- 10. attachments — 첨부파일
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
-- 11. policy_batch_history — 배치 작업 이력
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
-- 12. app_logs — 앱 에러/요청 로그
-- ------------------------------------------------------------
CREATE TABLE app_logs (
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
    KEY idx_app_logs_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '앱 로그 (기한 지나면 삭제)';

-- ------------------------------------------------------------
-- 13. search_histories — 검색 로그
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
