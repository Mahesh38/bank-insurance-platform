package com.bank.platform.quotation.config;

import com.bank.platform.quotation.adapter.crypto.AesGcmEnvelopeCipher;
import com.bank.platform.quotation.adapter.crypto.DataKeyProvider;
import com.bank.platform.quotation.adapter.crypto.KmsDataKeyProvider;
import com.bank.platform.quotation.adapter.crypto.LocalDataKeyProvider;
import com.bank.platform.quotation.adapter.dynamodb.QuoteTableSettings;
import com.bank.platform.quotation.domain.EnvelopeCipher;
import com.bank.platform.quotation.domain.WorkingRetention;
import java.net.URI;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;

/**
 * DynamoDB and envelope-encryption wiring for DB-DEC-0005. Credentials come from the default chain
 * (IRSA in EKS). The endpoint override and the static local key are refused outside the {@code
 * local} / {@code test} profiles (schemas/20 §5) — neither may reach a real environment by a stray
 * environment variable.
 */
@Configuration
public class WorkingStoreConfig {

  private static final Profiles LOCAL_OR_TEST = Profiles.of("local", "test");

  @Bean
  QuoteTableSettings quoteTableSettings(WorkingStoreProperties p) {
    return new QuoteTableSettings(
        p.tableName(),
        new WorkingRetention(p.retentionWorking(), p.retentionAfterMaterialize()),
        p.idempotencyTtl(),
        p.outboxTtl(),
        p.outboxPendingShards());
  }

  @Bean(destroyMethod = "close")
  DynamoDbClient dynamoDbClient(WorkingStoreProperties p, Environment environment) {
    DynamoDbClientBuilder builder =
        DynamoDbClient.builder()
            .region(Region.of(p.region()))
            .httpClient(UrlConnectionHttpClient.create());
    if (p.endpointOverride() != null && !p.endpointOverride().isBlank()) {
      requireLocalOrTest(environment, "quotation.working-store.endpoint-override");
      builder.endpointOverride(URI.create(p.endpointOverride()));
    }
    return builder.build();
  }

  @Bean
  DataKeyProvider dataKeyProvider(
      EnvelopeCryptoProperties p,
      WorkingStoreProperties store,
      Environment environment,
      Clock clock) {
    return switch (p.provider()) {
      case "local" -> {
        requireLocalOrTest(environment, "quotation.envelope.provider=local");
        yield new LocalDataKeyProvider(p.localKey(), p.localKeyId());
      }
      case "kms" -> {
        KmsClient kms =
            KmsClient.builder()
                .region(Region.of(store.region()))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
        yield new KmsDataKeyProvider(
            kms, p.kmsKeyId(), "quotation-service", p.dataKeyCacheTtl(), clock);
      }
      default ->
          throw new IllegalStateException("unknown quotation.envelope.provider " + p.provider());
    };
  }

  @Bean
  EnvelopeCipher envelopeCipher(DataKeyProvider keys) {
    return new AesGcmEnvelopeCipher(keys);
  }

  private static void requireLocalOrTest(Environment environment, String setting) {
    if (!environment.acceptsProfiles(LOCAL_OR_TEST)) {
      throw new IllegalStateException(setting + " is only allowed in the local or test profile");
    }
  }
}
