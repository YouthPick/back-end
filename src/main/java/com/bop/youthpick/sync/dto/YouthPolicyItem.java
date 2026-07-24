package com.bop.youthpick.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 온통청년 정책 1건 — API 원문을 String 그대로 보존한다. 타입 변환(숫자/날짜/Boolean)은 PolicyMapper(Phase 2)의 책임. 모든
 * 필드에 @JsonProperty 명시: 키 오타 시 조용히 null이 되는 사고 방지. 주의: 명세서 표기는 sBizCd지만 실제 JSON 키는 소문자 sbizCd (명세
 * §6-1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YouthPolicyItem(
        // 식별·표시
        @JsonProperty("plcyNo") String plcyNo,
        @JsonProperty("plcyNm") String plcyNm,
        @JsonProperty("plcyExplnCn") String plcyExplnCn,
        @JsonProperty("plcySprtCn") String plcySprtCn,

        // 분류·검색
        @JsonProperty("lclsfNm") String lclsfNm,
        @JsonProperty("mclsfNm") String mclsfNm,
        @JsonProperty("plcyKywdNm") String plcyKywdNm,

        // 지역 (→ PolicyRegion 정규화)
        @JsonProperty("zipCd") String zipCd,

        // 자격 — 나이·소득
        @JsonProperty("sprtTrgtMinAge") String sprtTrgtMinAge,
        @JsonProperty("sprtTrgtMaxAge") String sprtTrgtMaxAge,
        @JsonProperty("sprtTrgtAgeLmtYn") String sprtTrgtAgeLmtYn,
        @JsonProperty("earnCndSeCd") String earnCndSeCd,
        @JsonProperty("earnMinAmt") String earnMinAmt,
        @JsonProperty("earnMaxAmt") String earnMaxAmt,
        @JsonProperty("earnEtcCn") String earnEtcCn,

        // 자격 — 코드 (콤마 다중값 원문 보존)
        @JsonProperty("jobCd") String jobCd,
        @JsonProperty("schoolCd") String schoolCd,
        @JsonProperty("mrgSttsCd") String mrgSttsCd,
        @JsonProperty("plcyMajorCd") String plcyMajorCd,
        @JsonProperty("sbizCd") String sbizCd,

        // 상태·기간
        @JsonProperty("aplyPrdSeCd") String aplyPrdSeCd,
        @JsonProperty("aplyYmd") String aplyYmd,
        @JsonProperty("bizPrdBgngYmd") String bizPrdBgngYmd,
        @JsonProperty("bizPrdEndYmd") String bizPrdEndYmd,
        @JsonProperty("bizPrdEtcCn") String bizPrdEtcCn,

        // 규모
        @JsonProperty("sprtSclCnt") String sprtSclCnt,
        @JsonProperty("sprtArvlSeqYn") String sprtArvlSeqYn,
        @JsonProperty("sprtSclLmtYn") String sprtSclLmtYn,

        // 링크·신청
        @JsonProperty("aplyUrlAddr") String aplyUrlAddr,
        @JsonProperty("refUrlAddr1") String refUrlAddr1,
        @JsonProperty("refUrlAddr2") String refUrlAddr2,
        @JsonProperty("sbmsnDcmntCn") String sbmsnDcmntCn,
        @JsonProperty("plcyAplyMthdCn") String plcyAplyMthdCn,
        @JsonProperty("sprvsnInstCdNm") String sprvsnInstCdNm,
        @JsonProperty("operInstCdNm") String operInstCdNm,

        // 상세 보조
        @JsonProperty("addAplyQlfcCndCn") String addAplyQlfcCndCn,
        @JsonProperty("ptcpPrpTrgtCn") String ptcpPrpTrgtCn,
        @JsonProperty("srngMthdCn") String srngMthdCn,
        @JsonProperty("etcMttrCn") String etcMttrCn,
        @JsonProperty("plcyAprvSttsCd") String plcyAprvSttsCd,

        // 메타·정렬
        @JsonProperty("inqCnt") String inqCnt,
        @JsonProperty("frstRegDt") String frstRegDt,
        @JsonProperty("lastMdfcnDt") String lastMdfcnDt) {}
