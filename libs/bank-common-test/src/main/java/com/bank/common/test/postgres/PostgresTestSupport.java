package com.bank.common.test.postgres;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * S08-E03-S01 — shared PostgreSQL Testcontainers factory for service integration tests.
 *
 * <p>Callers own the JUnit lifecycle ({@code @Container} / {@code @Testcontainers}) and bind
 * datasource properties via {@link #register(PostgreSQLContainer, DynamicPropertyRegistry)}.
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
    PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(imageName));
    applyCredentials(container::withDatabaseName, container::withUsername, container::withPassword);
    return container;
  }

  static void applyCredentials(
      Consumer<String> databaseName, Consumer<String> username, Consumer<String> password) {
    databaseName.accept(DEFAULT_DATABASE);
    username.accept(DEFAULT_USERNAME);
    password.accept(DEFAULT_PASSWORD);
  }

  public static void register(PostgreSQLContainer<?> postgres, DynamicPropertyRegistry registry) {
    bindDatasource(registry, postgres::getJdbcUrl, postgres::getUsername, postgres::getPassword);
  }

  static void bindDatasource(
      DynamicPropertyRegistry registry,
      Supplier<Object> url,
      Supplier<Object> username,
      Supplier<Object> password) {
    registry.add("spring.datasource.url", url);
    registry.add("spring.datasource.username", username);
    registry.add("spring.datasource.password", password);
    registry.add("spring.datasource.driver-class-name", PostgresTestSupport::postgresDriver);
  }

  static String postgresDriver() {
    return "org.postgresql.Driver";
  }
}
