package com.bank.platform.proposal.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.platform.proposal.domain.SealedPayload;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

class EnvelopeCryptoTest {

  private static final String KEY = "p7MSmdyOLeyBue5/ZJpPP3TE/ZQrLr4WSPjrhS0d0F8=";
  private static final byte[] SECRET = "nominee: Asha Rao".getBytes(StandardCharsets.UTF_8);

  @Test
  void localRoundTripIsBoundToItsContext() {
    AesGcmEnvelopeCipher cipher = new AesGcmEnvelopeCipher(new LocalDataKeyProvider(KEY, "t"));

    SealedPayload sealed = cipher.seal(SECRET, "PROPOSAL#1");

    assertThat(sealed.encryptionKeyId()).isEqualTo("local:t");
    assertThat(new String(sealed.ciphertext(), StandardCharsets.ISO_8859_1)).doesNotContain("Asha");
    assertThat(cipher.open(sealed, "PROPOSAL#1")).isEqualTo(SECRET);
    assertThatThrownBy(() -> cipher.open(sealed, "PROPOSAL#2"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> cipher.open(new SealedPayload(sealed.ciphertext(), "local:x"), "PROPOSAL#1"))
        .isInstanceOf(IllegalStateException.class);
    byte[] badFormat = sealed.ciphertext().clone();
    badFormat[0] = 9;
    assertThatThrownBy(() -> cipher.open(new SealedPayload(badFormat, "local:t"), "PROPOSAL#1"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void localKeyMustBe256Bits() {
    assertThatThrownBy(() -> new LocalDataKeyProvider("AAAA", "t"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void kmsDataKeysAreGeneratedOnceAndCachedForTheTtl() {
    byte[] plain = new byte[32];
    new SecureRandom().nextBytes(plain);
    byte[] blob = "encrypted-data-key".getBytes(StandardCharsets.UTF_8);
    KmsClient kms = mock(KmsClient.class);
    when(kms.generateDataKey(any(Consumer.class)))
        .thenReturn(
            GenerateDataKeyResponse.builder()
                .plaintext(SdkBytes.fromByteArray(plain))
                .ciphertextBlob(SdkBytes.fromByteArray(blob))
                .build());
    when(kms.decrypt(any(Consumer.class)))
        .thenReturn(DecryptResponse.builder().plaintext(SdkBytes.fromByteArray(plain)).build());
    MutableClock clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
    KmsDataKeyProvider provider =
        new KmsDataKeyProvider(
            kms, "alias/test-proposal-working", "proposal-service", Duration.ofMinutes(5), clock);
    AesGcmEnvelopeCipher cipher = new AesGcmEnvelopeCipher(provider);

    SealedPayload a = cipher.seal(SECRET, "c");
    SealedPayload b = cipher.seal(SECRET, "c");
    assertThat(a.encryptionKeyId()).isEqualTo("alias/test-proposal-working");
    verify(kms, times(1)).generateDataKey(any(Consumer.class));

    assertThat(cipher.open(a, "c")).isEqualTo(SECRET);
    assertThat(cipher.open(b, "c")).isEqualTo(SECRET);
    verify(kms, times(1)).decrypt(any(Consumer.class));

    clock.advance(Duration.ofMinutes(6));
    cipher.seal(SECRET, "c");
    assertThat(cipher.open(a, "c")).isEqualTo(SECRET);
    verify(kms, times(2)).generateDataKey(any(Consumer.class));
    verify(kms, times(2)).decrypt(any(Consumer.class));
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
