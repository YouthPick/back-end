package com.bop.youthpick.log.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

/**
 * application_logs 테이블에 로그를 적재하는 전용 Appender. Log4j2 내장 JDBC Appender는 컬럼 값을 항상 문자열로 바인딩해 MDC 값이 없을
 * 때 빈 문자열("")을 BIGINT user_id 컬럼에 넣으려다 실패하므로, 직접 PreparedStatement로 타입을 맞춰 적재한다.
 */
@Plugin(name = "ApplicationLog", category = "Core", elementType = "appender", printObject = true)
public final class ApplicationLogAppender extends AbstractAppender {

    private static final String INSERT_SQL =
            "INSERT INTO application_logs "
                    + "(level, message, trace_id, method, uri, ip, exception_class, exception_message, stack_trace, user_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private ApplicationLogAppender(String name, Filter filter) {
        super(name, filter, null, true, Property.EMPTY_ARRAY);
    }

    @PluginFactory
    public static ApplicationLogAppender createAppender(
            @PluginAttribute("name") String name, @PluginElement("Filter") Filter filter) {
        return new ApplicationLogAppender(name, filter);
    }

    @Override
    public void append(LogEvent event) {
        DataSource dataSource = ApplicationLogDataSource.getDataSource();
        Throwable thrown = event.getThrown();
        ReadOnlyStringMap context = event.getContextData();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, event.getLevel().toString());
            statement.setString(2, event.getMessage().getFormattedMessage());
            statement.setString(3, context.getValue("traceId"));
            statement.setString(4, context.getValue("method"));
            statement.setString(5, context.getValue("uri"));
            statement.setString(6, context.getValue("ip"));
            statement.setString(7, thrown == null ? null : thrown.getClass().getName());
            statement.setString(8, thrown == null ? null : thrown.getMessage());
            statement.setString(9, thrown == null ? null : stackTraceOf(thrown));
            setNullableLong(statement, 10, context.getValue("userId"));
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("application_logs 적재에 실패했습니다.", e);
        }
    }

    private void setNullableLong(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.BIGINT);
            return;
        }
        try {
            statement.setLong(index, Long.parseLong(value));
        } catch (NumberFormatException e) {
            LOGGER.warn("user_id MDC 값이 숫자가 아니라 NULL로 적재합니다: {}", value);
            statement.setNull(index, Types.BIGINT);
        }
    }

    private static String stackTraceOf(Throwable thrown) {
        StringWriter writer = new StringWriter();
        thrown.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
