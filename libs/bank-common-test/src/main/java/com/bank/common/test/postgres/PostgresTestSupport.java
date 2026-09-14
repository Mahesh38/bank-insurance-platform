package com.bank.common.test.postgres;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * S08-E03-S01 — shared PostgreSQL Testcontainers factory for service integration tests.
 *
 * <p>Callers own the JUnit lifecycle ({@code @Container} / {@code @Testcontainers}) and bind
 * datasource properties via {@link #register(PostgreSQLContainer,
 * org.springframework.test.context.DynamicPropertyRegistry)}.
 */
public final class PostgresTestSupport {

  public static final String DEFAULT_IMAGE = "postgres:16-alpine";
  public static final String DEFAULT_DATABASE = "bank_it";
  public static final String DEFAULT_USERNAME = "bank";
  public static final String DEFAULT_PASSWORD = "bank";

  private PostgresTestSupport() {}

  public static PostgreSQLContainer<?> create() {
    return create(DEFAULT_IMAGE);
  }

  @SuppressWarnings("resource")
  public static PostgreSQLContainer<?> create(String imageName) {
    return new PostgreSQLContainer<>(DockerImageName.parse(imageName))
        .withDatabaseName(DEFAULT_DATABASE)
        .withUsername(DEFAULT_USERNAME)
        .withPassword(DEFAULT_PASSWORD);
  }

  /**
   * Binds Spring datasource properties to a started container. Driver is forced to PostgreSQL so
   * profiles that default to H2 cannot silently win.
   */
  public static void register(
      PostgreSQLContainer<?> postgres,
      org.springframework.test.context.DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }
}
