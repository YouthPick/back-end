package com.bop.youthpick.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code @Pattern}이 컴파일 상수만 받는 탓에 enum 상수 목록을 {@code PATTERN} 문자열로 한 번 더 적어야 한다. 둘이 어긋나면 검증이 조용히
 * 헐거워지거나(누락된 상수를 거부) 반대로 없는 코드를 통과시키므로, 여기서 일치를 강제한다.
 */
class ProfileCodePatternTest {

    @Test
    void 취업상태_PATTERN은_enum_상수와_정확히_같다() {
        assertThat(alternatives(EmploymentStatus.PATTERN))
                .containsExactlyInAnyOrderElementsOf(names(EmploymentStatus.values()));
    }

    @Test
    void 학력_PATTERN은_enum_상수와_정확히_같다() {
        assertThat(alternatives(EducationLevel.PATTERN))
                .containsExactlyInAnyOrderElementsOf(names(EducationLevel.values()));
    }

    @Test
    void 결혼여부_전공_특화조건_PATTERN도_enum_상수와_정확히_같다() {
        assertThat(alternatives(MaritalStatus.PATTERN))
                .containsExactlyInAnyOrderElementsOf(names(MaritalStatus.values()));
        assertThat(alternatives(Major.PATTERN))
                .containsExactlyInAnyOrderElementsOf(names(Major.values()));
        assertThat(alternatives(SpecialCondition.PATTERN))
                .containsExactlyInAnyOrderElementsOf(names(SpecialCondition.values()));
    }

    @Test
    void 모든_상수가_자기_PATTERN에_매칭된다() {
        for (EmploymentStatus status : EmploymentStatus.values()) {
            assertThat(status.name().matches(EmploymentStatus.PATTERN)).isTrue();
        }
        for (EducationLevel level : EducationLevel.values()) {
            assertThat(level.name().matches(EducationLevel.PATTERN)).isTrue();
        }
    }

    @Test
    void 어휘에_없는_값은_PATTERN에_매칭되지_않는다() {
        // UNIVERSITY는 온보딩 선택지에 없는데도 검증이 없던 시절 테스트 픽스처로 쓰이던 값이다.
        assertThat("UNIVERSITY".matches(EducationLevel.PATTERN)).isFalse();
        assertThat("MILITARY".matches(EmploymentStatus.PATTERN)).isFalse();
        assertThat("".matches(EmploymentStatus.PATTERN)).isFalse();
    }

    private static List<String> alternatives(String pattern) {
        return Arrays.stream(pattern.split("\\|")).toList();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
