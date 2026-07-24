package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.policy.entity.Policy;
import jakarta.persistence.Column;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PolicyMapper의 길이 상한 상수와 Policy {@code @Column(length = ...)}가 어긋나면 실패한다(#208). 둘 중 하나만 고치고 다른 쪽을
 * 깜빡하면 여기서 드리프트를 잡아낸다 — 매퍼 상수가 실제 컬럼보다 크면 여전히 Data truncation으로 정책 1건을 통째로 잃고, 작으면 불필요하게 잘려 나간다.
 */
class PolicyMapperLengthLimitsTest {

    private record LengthLimit(String mapperConstant, String entityField) {}

    /** PolicyMapper의 길이 상한 상수 ↔ Policy 엔티티 필드 매핑. 새 길이 가드를 추가하면 이 목록에도 반드시 추가한다. */
    private static final List<LengthLimit> LIMITS =
            List.of(
                    new LengthLimit("POLICY_NO_MAX_LENGTH", "policyNo"),
                    new LengthLimit("TITLE_MAX_LENGTH", "title"),
                    new LengthLimit("DESCRIPTION_MAX_LENGTH", "description"),
                    new LengthLimit("KEYWORDS_MAX_LENGTH", "keywords"),
                    new LengthLimit("CATEGORY_MAX_LENGTH", "category"),
                    new LengthLimit("MIDDLE_CATEGORY_MAX_LENGTH", "middleCategory"),
                    new LengthLimit("ORGANIZATION_NAME_MAX_LENGTH", "organizationName"),
                    new LengthLimit("JOB_CODES_MAX_LENGTH", "jobCodes"),
                    new LengthLimit("SCHOOL_CODES_MAX_LENGTH", "schoolCodes"),
                    new LengthLimit("INCOME_CONDITION_CODE_MAX_LENGTH", "incomeConditionCode"),
                    new LengthLimit("MARITAL_STATUS_CODE_MAX_LENGTH", "maritalStatusCode"),
                    new LengthLimit("MAJOR_CODES_MAX_LENGTH", "majorCodes"),
                    new LengthLimit("SPECIALIZATION_CODES_MAX_LENGTH", "specializationCodes"),
                    new LengthLimit("APPLICATION_PERIOD_TYPE_MAX_LENGTH", "applicationPeriodType"),
                    new LengthLimit("APPLICATION_PERIOD_RAW_MAX_LENGTH", "applicationPeriodRaw"),
                    new LengthLimit("APPLICATION_URL_MAX_LENGTH", "applicationUrl"),
                    new LengthLimit("REFERENCE_URL_MAX_LENGTH", "referenceUrl1"),
                    new LengthLimit("REFERENCE_URL_MAX_LENGTH", "referenceUrl2"),
                    new LengthLimit("AGE_LIMIT_FLAG_MAX_LENGTH", "ageLimitFlag"),
                    new LengthLimit(
                            "OPERATING_INSTITUTION_NAME_MAX_LENGTH", "operatingInstitutionName"),
                    new LengthLimit("APPROVAL_STATUS_CODE_MAX_LENGTH", "approvalStatusCode"));

    @Test
    @DisplayName("X: 매퍼 길이 상한 상수는 Policy @Column(length)와 정확히 일치해야 한다 — 드리프트 방지 (#208)")
    void mapperLengthConstantsMatchEntityColumnLength() throws Exception {
        for (LengthLimit limit : LIMITS) {
            int mapperValue = readMapperConstant(limit.mapperConstant());
            int entityValue = readEntityColumnLength(limit.entityField());
            assertThat(mapperValue)
                    .as(
                            "PolicyMapper.%s(%d)는 Policy.%s의 @Column(length=%d)와 일치해야 한다",
                            limit.mapperConstant(), mapperValue, limit.entityField(), entityValue)
                    .isEqualTo(entityValue);
        }
    }

    private static int readMapperConstant(String name) throws Exception {
        Field field = PolicyMapper.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static int readEntityColumnLength(String fieldName) throws Exception {
        Field field = Policy.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column).as("Policy.%s 에 @Column 애노테이션이 없다", fieldName).isNotNull();
        return column.length();
    }
}
