package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.user.entity.EducationLevel;
import com.bop.youthpick.user.entity.EmploymentStatus;
import com.bop.youthpick.user.entity.Major;
import com.bop.youthpick.user.entity.MaritalStatus;
import com.bop.youthpick.user.entity.SpecialCondition;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * 온보딩 어휘({@link EmploymentStatus}/{@link EducationLevel}) ↔ 온통청년 코드 매핑이 어긋나지 않도록 잡는 가드.
 *
 * <p>매핑이 빠지면 예외가 나는 게 아니라 <b>조용히 해당 축 0점</b>이 된다(실제로 그 상태로, 취업·학력 축이 한 번도 매칭되지 않은 채 전체 정책이 추천으로
 * 나갔다). 그래서 런타임이 아니라 빌드에서 잡는다 — enum에 상수를 추가하고 매퍼를 갱신하지 않으면 여기서 깨진다.
 */
class YouthPolicyCodeMapperTest {

    @Test
    void 모든_취업상태_상수가_매핑돼_있다() {
        assertThat(YouthPolicyCodeMapper.supportedEmploymentStatuses())
                .containsExactlyInAnyOrderElementsOf(names(EmploymentStatus.values()));
    }

    @Test
    void 모든_학력_상수가_매핑돼_있다() {
        assertThat(YouthPolicyCodeMapper.supportedEducationLevels())
                .containsExactlyInAnyOrderElementsOf(names(EducationLevel.values()));
    }

    @Test
    void 모든_취업상태가_서로_다른_jobCd로_변환된다() {
        List<String> jobCodes =
                Arrays.stream(EmploymentStatus.values())
                        .map(status -> YouthPolicyCodeMapper.toJobCode(status.name()))
                        .toList();

        assertThat(jobCodes).doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    void 모든_학력이_서로_다른_schoolCd로_변환된다() {
        List<String> schoolCodes =
                Arrays.stream(EducationLevel.values())
                        .map(level -> YouthPolicyCodeMapper.toSchoolCode(level.name()))
                        .toList();

        assertThat(schoolCodes).doesNotContainNull().doesNotHaveDuplicates();
    }

    /** 제한없음은 정책이 "대상을 안 가린다"는 표시일 뿐 사용자가 고를 수 있는 상태가 아니다 — 변환 결과로 나오면 자격 판정이 무너진다. */
    @Test
    void 제한없음_코드는_어떤_선택지로도_변환되지_않는다() {
        assertThat(
                        Arrays.stream(EmploymentStatus.values())
                                .map(status -> YouthPolicyCodeMapper.toJobCode(status.name())))
                .doesNotContain(YouthPolicyCodeMapper.JOB_CODE_UNRESTRICTED);
        assertThat(
                        Arrays.stream(EducationLevel.values())
                                .map(level -> YouthPolicyCodeMapper.toSchoolCode(level.name())))
                .doesNotContain(YouthPolicyCodeMapper.SCHOOL_CODE_UNRESTRICTED);
    }

    @Test
    void 모든_결혼여부_상수가_서로_다른_코드로_매핑돼_있다() {
        assertThat(YouthPolicyCodeMapper.supportedMaritalStatuses())
                .containsExactlyInAnyOrderElementsOf(names(MaritalStatus.values()));
        assertThat(codesOf(MaritalStatus.values(), YouthPolicyCodeMapper::toMaritalCode))
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .doesNotContain(YouthPolicyCodeMapper.MARITAL_CODE_UNRESTRICTED);
    }

    @Test
    void 모든_전공_상수가_서로_다른_코드로_매핑돼_있다() {
        assertThat(YouthPolicyCodeMapper.supportedMajors())
                .containsExactlyInAnyOrderElementsOf(names(Major.values()));
        assertThat(codesOf(Major.values(), YouthPolicyCodeMapper::toMajorCode))
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .doesNotContain(YouthPolicyCodeMapper.MAJOR_CODE_UNRESTRICTED);
    }

    @Test
    void 모든_특화조건_상수가_서로_다른_코드로_매핑돼_있다() {
        assertThat(YouthPolicyCodeMapper.supportedSpecialConditions())
                .containsExactlyInAnyOrderElementsOf(names(SpecialCondition.values()));
        assertThat(codesOf(SpecialCondition.values(), YouthPolicyCodeMapper::toSpecializationCode))
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .doesNotContain(YouthPolicyCodeMapper.SPECIALIZATION_CODE_UNRESTRICTED);
    }

    @Test
    void 모르는_값이나_null은_변환하지_않고_null을_돌려준다() {
        assertThat(YouthPolicyCodeMapper.toJobCode("NOT_A_REAL_STATUS")).isNull();
        assertThat(YouthPolicyCodeMapper.toSchoolCode("NOT_A_REAL_LEVEL")).isNull();
        assertThat(YouthPolicyCodeMapper.toJobCode(null)).isNull();
        assertThat(YouthPolicyCodeMapper.toSchoolCode(null)).isNull();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private static List<String> codesOf(Enum<?>[] values, UnaryOperator<String> toCode) {
        return Arrays.stream(values).map(Enum::name).map(toCode).toList();
    }
}
