package com.bop.youthpick.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayMigrationLocationsTest {

    @Test
    void migration과_seed_위치를_함께_해석할_수_있다() {
        Flyway flyway =
                Flyway.configure()
                        .dataSource("jdbc:h2:mem:flyway-location-test;MODE=MySQL", "sa", "")
                        .locations("classpath:db/migration", "classpath:db/seed")
                        .load();

        assertThatCode(flyway::info).doesNotThrowAnyException();
    }
}
