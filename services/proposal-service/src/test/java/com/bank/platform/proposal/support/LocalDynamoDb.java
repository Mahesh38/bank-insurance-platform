package com.bank.platform.proposal.support;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.local.shared.access.AmazonDynamoDBLocal;
import com.bank.platform.proposal.adapter.dynamodb.DynamoOutbox;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * In-process DynamoDB Local (no Docker): one instance per test JVM. The table fixture mirrors the
 * Terraform definition in schemas/20 §1 — PK/SK, GSI1 by journey.
 */
public final class LocalDynamoDb {

  public static final String TABLE = "test-proposal-working";

  private static AmazonDynamoDBLocal local;
  private static DynamoDbClient client;

  private LocalDynamoDb() {}

  public static synchronized DynamoDbClient client() {
    if (client == null) {
      local = DynamoDBEmbedded.create();
      client = local.dynamoDbClient();
      createTable(client, TABLE);
    }
    return client;
  }

  public static void createTable(DynamoDbClient dynamo, String name) {
    try {
      dynamo.createTable(
          r ->
              r.tableName(name)
                  .billingMode(BillingMode.PAY_PER_REQUEST)
                  .keySchema(key("PK", KeyType.HASH), key("SK", KeyType.RANGE))
                  .attributeDefinitions(
                      attr("PK"),
                      attr("SK"),
                      attr("GSI1PK"),
                      attr("GSI1SK"),
                      attr(DynamoOutbox.PENDING_PK),
                      attr(DynamoOutbox.PENDING_SK))
                  .globalSecondaryIndexes(
                      GlobalSecondaryIndex.builder()
                          .indexName("GSI1")
                          .keySchema(key("GSI1PK", KeyType.HASH), key("GSI1SK", KeyType.RANGE))
                          .projection(
                              Projection.builder()
                                  .projectionType(ProjectionType.INCLUDE)
                                  .nonKeyAttributes("proposalId", "state", "version")
                                  .build())
                          .build(),
                      // Sparse: only unpublished OUTBOX items carry the keys (ADR-025 §3).
                      GlobalSecondaryIndex.builder()
                          .indexName(DynamoOutbox.PENDING_INDEX)
                          .keySchema(
                              key(DynamoOutbox.PENDING_PK, KeyType.HASH),
                              key(DynamoOutbox.PENDING_SK, KeyType.RANGE))
                          .projection(
                              Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                          .build()));
    } catch (ResourceInUseException e) {
      // already created by another test class
    }
  }

  private static KeySchemaElement key(String name, KeyType type) {
    return KeySchemaElement.builder().attributeName(name).keyType(type).build();
  }

  private static AttributeDefinition attr(String name) {
    return AttributeDefinition.builder()
        .attributeName(name)
        .attributeType(ScalarAttributeType.S)
        .build();
  }
}
