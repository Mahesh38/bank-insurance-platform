package com.bank.platform.quotation.adapter.crypto;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

/**
 * Data keys from the service's customer-managed KMS key (DB-DEC-0005 security implications).
 *
 * <p>Plaintext data keys are cached for {@code cacheTtl}: one generated key encrypts every write in
 * that window, and decrypted keys are reused for repeated reads of the same item. That keeps KMS
 * off the hot path of page-by-page saves without holding any key for long.
 */
public class KmsDataKeyProvider implements DataKeyProvider {

  private static final int MAX_CACHED_KEYS = 256;

  private record Cached(byte[] plaintext, byte[] encrypted, Instant expiresAt) {}

  private final KmsClient kms;
  private final String keyId;
  private final Map<String, String> encryptionContext;
  private final Duration cacheTtl;
  private final Clock clock;
  private final Map<String, Cached> decrypted = new ConcurrentHashMap<>();
  private volatile Cached current;

  public KmsDataKeyProvider(
      KmsClient kms, String keyId, String serviceName, Duration cacheTtl, Clock clock) {
    this.kms = kms;
    this.keyId = keyId;
    this.encryptionContext = Map.of("service", serviceName);
    this.cacheTtl = cacheTtl;
    this.clock = clock;
  }

  @Override
  public DataKey current() {
    Instant now = clock.instant();
    Cached cached = current;
    if (cached == null || !now.isBefore(cached.expiresAt())) {
      GenerateDataKeyResponse generated =
          kms.generateDataKey(
              r ->
                  r.keyId(keyId).keySpec(DataKeySpec.AES_256).encryptionContext(encryptionContext));
      cached =
          new Cached(
              generated.plaintext().asByteArray(),
              generated.ciphertextBlob().asByteArray(),
              now.plus(cacheTtl));
      current = cached;
    }
    return new DataKey(cached.plaintext(), cached.encrypted(), keyId);
  }

  @Override
  public byte[] decrypt(byte[] encryptedKey, String masterKeyId) {
    Instant now = clock.instant();
    String cacheKey = Base64.getEncoder().encodeToString(encryptedKey);
    Cached cached = decrypted.get(cacheKey);
    if (cached != null && now.isBefore(cached.expiresAt())) {
      return cached.plaintext();
    }
    DecryptResponse response =
        kms.decrypt(
            r ->
                r.keyId(masterKeyId)
                    .ciphertextBlob(SdkBytes.fromByteArray(encryptedKey))
                    .encryptionContext(encryptionContext));
    byte[] plaintext = response.plaintext().asByteArray();
    if (decrypted.size() >= MAX_CACHED_KEYS) {
      decrypted.values().removeIf(c -> !now.isBefore(c.expiresAt()));
      if (decrypted.size() >= MAX_CACHED_KEYS) {
        decrypted.clear();
      }
    }
    decrypted.put(cacheKey, new Cached(plaintext, encryptedKey, now.plus(cacheTtl)));
    return plaintext;
  }
}
