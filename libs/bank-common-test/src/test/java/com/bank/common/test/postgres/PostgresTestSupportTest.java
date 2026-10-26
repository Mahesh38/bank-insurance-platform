package com.bank.common.test.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;

class PostgresTestSupportTest {

  @Test
  void applyCredentialsUsesTheSharedDatabaseAccount() {
    List<String> database = new ArrayList<>();
    List<String> username = new ArrayList<>();
    List<String> password = new ArrayList<>();
    PostgresTestSupport.applyCredentials(database::add, username::add, password::add);
    assertThat(database).containsExactly(PostgresTestSupport.DEFAULT_DATABASE);
    assertThat(username).containsExactly(PostgresTestSupport.DEFAULT_USERNAME);
    assertThat(password).containsExactly(PostgresTestSupport.DEFAULT_PASSWORD);
  }

  @Test
  void bindDatasourceRegistersThePostgresDriver() {
    Map<String, Supplier<Object>> bound = new LinkedHashMap<>();
    DynamicPropertyRegistry registry = bound::put;
    PostgresTestSupport.bindDatasource(
        registry, () -> "jdbc:postgresql://localhost/bank_it", () -> "bank", () -> "bank");
    assertThat(bound)
        .containsKeys(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.datasource.driver-class-name");
    assertThat(bound.get("spring.datasource.url").get())
        .isEqualTo("jdbc:postgresql://localhost/bank_it");
    assertThat(bound.get("spring.datasource.username").get()).isEqualTo("bank");
    assertThat(bound.get("spring.datasource.password").get()).isEqualTo("bank");
    assertThat(bound.get("spring.datasource.driver-class-name").get())
        .isEqualTo("org.postgresql.Driver");
  }

  @Test
  void constructorIsPrivate() throws Exception {
    Constructor<PostgresTestSupport> constructor =
        PostgresTestSupport.class.getDeclaredConstructor();
    assertThat(constructor.canAccess(null)).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}
