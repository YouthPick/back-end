package com.bop.youthpick.admin.log.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.log.entity.SearchLog;
import com.bop.youthpick.log.repository.SearchLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminSearchLogSpecificationsTest {

    @Autowired private SearchLogRepository searchLogRepository;

    @Autowired private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        saveSearchLog("청년 정책", "청년정책", LocalDateTime.of(2026, 3, 15, 9, 0));
        saveSearchLog("주거 지원", "주거지원", LocalDateTime.of(2026, 6, 1, 9, 0));
        saveSearchLog("일자리", "일자리", LocalDateTime.of(2026, 6, 10, 9, 0));
    }

    // createdAt은 @CreatedDate + updatable=false라 auditing이 채운 뒤에는 JPA로 값을 바꿀 수 없다.
    // 날짜 필터 테스트를 위해 네이티브 쿼리로 직접 덮어쓴다.
    private void saveSearchLog(String query, String normalized, LocalDateTime createdAt) {
        SearchLog searchLog = BeanUtils.instantiateClass(SearchLog.class);
        ReflectionTestUtils.setField(searchLog, "query", query);
        ReflectionTestUtils.setField(searchLog, "normalized", normalized);
        ReflectionTestUtils.setField(searchLog, "resultCount", 0);
        SearchLog saved = searchLogRepository.saveAndFlush(searchLog);

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE search_histories SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void keyword로_query_또는_normalized를_부분일치_검색한다() {
        List<SearchLog> result =
                searchLogRepository.findAll(AdminSearchLogSpecifications.filter("주거", null, null));

        assertThat(result).extracting(SearchLog::getQuery).containsExactly("주거 지원");
    }

    @Test
    void 날짜범위로_필터링한다() {
        List<SearchLog> result =
                searchLogRepository.findAll(
                        AdminSearchLogSpecifications.filter(
                                null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(result).extracting(SearchLog::getQuery).containsExactly("청년 정책");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<SearchLog> result =
                searchLogRepository.findAll(AdminSearchLogSpecifications.filter(null, null, null));

        assertThat(result).hasSize(3);
    }
}
