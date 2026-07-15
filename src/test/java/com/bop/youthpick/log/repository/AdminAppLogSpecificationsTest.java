package com.bop.youthpick.log.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.log.entity.AppLog;
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
class AdminAppLogSpecificationsTest {

    @Autowired private AppLogRepository appLogRepository;

    @Autowired private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        saveAppLog("ERROR", "connection timeout", "trace-1", LocalDateTime.of(2026, 3, 15, 9, 0));
        saveAppLog("WARN", "slow query", "trace-2", LocalDateTime.of(2026, 6, 1, 9, 0));
        saveAppLog("INFO", "request completed", "trace-3", LocalDateTime.of(2026, 6, 10, 9, 0));
    }

    // createdAt은 @CreatedDate + updatable=false라 auditing이 채운 뒤에는 JPA로 값을 바꿀 수 없다.
    // 날짜 필터 테스트를 위해 네이티브 쿼리로 직접 덮어쓴다.
    private void saveAppLog(String level, String message, String traceId, LocalDateTime createdAt) {
        AppLog appLog = BeanUtils.instantiateClass(AppLog.class);
        ReflectionTestUtils.setField(appLog, "level", level);
        ReflectionTestUtils.setField(appLog, "message", message);
        ReflectionTestUtils.setField(appLog, "traceId", traceId);
        ReflectionTestUtils.setField(appLog, "uri", "/api/v1/policies");
        AppLog saved = appLogRepository.saveAndFlush(appLog);

        entityManager
                .getEntityManager()
                .createNativeQuery("UPDATE app_logs SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void logLevel로_필터링한다() {
        List<AppLog> result =
                appLogRepository.findAll(
                        AdminAppLogSpecifications.filter("WARN", null, null, null));

        assertThat(result).extracting(AppLog::getTraceId).containsExactly("trace-2");
    }

    @Test
    void keyword로_message_traceId_uri_부분일치를_검색한다() {
        List<AppLog> result =
                appLogRepository.findAll(
                        AdminAppLogSpecifications.filter(null, "timeout", null, null));

        assertThat(result).extracting(AppLog::getTraceId).containsExactly("trace-1");
    }

    @Test
    void 날짜범위로_필터링한다() {
        List<AppLog> result =
                appLogRepository.findAll(
                        AdminAppLogSpecifications.filter(
                                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(result).extracting(AppLog::getTraceId).containsExactly("trace-1");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<AppLog> result =
                appLogRepository.findAll(AdminAppLogSpecifications.filter(null, null, null, null));

        assertThat(result).hasSize(3);
    }
}
