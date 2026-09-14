package com.bank.common.test.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.test.PyramidTags;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@Tag(PyramidTags.TESTCONTAINERS)
@Tag(PyramidTags.INTEGRATION)
class PostgresTestSupportIT {

  @Container
  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES = PostgresTestSupport.create();

  @Test
  void create_startsQueryablePostgres() throws Exception {
    assertThat(POSTGRES.isRunning()).isTrue();
    try (Connection connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        ResultSet rs = connection.createStatement().executeQuery("SELECT 1")) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  void register_bindsSpringDatasourceProperties() {
    Map<String, Supplier<Object>> bound = new LinkedHashMap<>();
    DynamicPropertyRegistry registry = (name, valueSupplier) -> bound.put(name, valueSupplier);

    PostgresTestSupport.register(POSTGRES, registry);

    assertThat(bound.get("spring.datasource.url").get()).isEqualTo(POSTGRES.getJdbcUrl());
    assertThat(bound.get("spring.datasource.username").get()).isEqualTo(POSTGRES.getUsername());
    assertThat(bound.get("spring.datasource.password").get()).isEqualTo(POSTGRES.getPassword());
    assertThat(bound.get("spring.datasource.driver-class-name").get())
        .isEqualTo("org.postgresql.Driver");
  }
}
