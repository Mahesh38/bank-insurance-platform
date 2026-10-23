package com.bank.platform.quotation.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** Points the application at the in-process DynamoDB Local. */
@TestConfiguration
public class EmbeddedDynamoDbTestConfig {

  // Shared across contexts; the JVM owns its lifecycle, so Spring must not close it.
  @Bean(destroyMethod = "")
  @Primary
  DynamoDbClient embeddedDynamoDbClient() {
    return LocalDynamoDb.client();
  }
}
