package com.bop.youthpick.log.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * log4j2.xml의 JDBC Appender가 참조하는 application_logs 전용 소형 커넥션 풀. Spring 메인 데이터소스와 분리해 Spring 초기화 전에도
 * 동작한다.
 */
public final class ApplicationLogDataSource {

    private static volatile HikariDataSource dataSource;

    private ApplicationLogDataSource() {}

    public static DataSource getDataSource() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (ApplicationLogDataSource.class) {
                local = dataSource;
                if (local == null) {
                    local = build();
                    dataSource = local;
                }
            }
        }
        return local;
    }

    private static HikariDataSource build() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                env(
                        "DB_URL",
                        "jdbc:mysql://localhost:3306/youthpick?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul"));
        config.setUsername(env("DB_USERNAME", "youthpick"));
        config.setPassword(env("DB_PASSWORD", "youthpick"));
        config.setPoolName("application-log-jdbc-appender");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(2000);
        return new HikariDataSource(config);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
