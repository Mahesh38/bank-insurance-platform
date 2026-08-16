package com.bank.workforce.bff.session;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security properties of the token vault.
 *
 * <p>This cipher is what makes the token-hiding BFF pattern real. It encrypts the OAuth tokens
 * that WS-2 standing constraint "Flutter never receives OAuth tokens" says must stay
 * server-side, so its failure modes matter more than its happy path — and the happy path is
 * already covered by {@code TokenVaultCipherTest}.
 *
 * <p>What is asserted here is the set of properties an attacker would try to break:
 * that a truncated or tampered ciphertext is rejected rather than partially decrypted, that a
 * different key cannot read a session, that a short key is refused at construction rather than
 * silently weakening the cipher, and that ciphertexts are non-deterministic so two identical
 * sessions are not linkable.
 */
class TokenVaultCipherSecurityTest {

    private static final String KEY_A = base64Key((byte) 0x11);
    private static final String KEY_B = base64Key((byte) 0x22);

    private final ObjectMapper objectMapper = new ObjectMapper();

    record SessionPayload(String subject, String accessToken, String refreshToken) {}

    private static final SessionPayload PAYLOAD =
        new SessionPayload("rm-e123", "access-token-value", "refresh-token-value");

    @Test
    @DisplayName("a session round-trips through encrypt/decrypt unchanged")
    void roundTripsPayload() {
        TokenVaultCipher cipher = cipher(KEY_A);

        String encrypted = cipher.encrypt(PAYLOAD);

        assertThat(cipher.decrypt(encrypted, SessionPayload.class)).isEqualTo(PAYLOAD);
    }

    @Test
    @DisplayName("the ciphertext does not leak the token values it protects")
    void ciphertextDoesNotContainPlaintext() {
        TokenVaultCipher cipher = cipher(KEY_A);

        String encrypted = cipher.encrypt(PAYLOAD);

        assertThat(encrypted)
            .doesNotContain("access-token-value")
            .doesNotContain("refresh-token-value")
            .doesNotContain("rm-e123");
    }

    @Test
    @DisplayName("encrypting the same session twice yields different ciphertexts (fresh IV per call)")
    void encryptionIsNonDeterministic() {
        TokenVaultCipher cipher = cipher(KEY_A);

        // A deterministic ciphertext would let an observer tell that two cookies carry the
        // same session without decrypting either of them.
        assertThat(cipher.encrypt(PAYLOAD)).isNotEqualTo(cipher.encrypt(PAYLOAD));
    }

    @Test
    @DisplayName("a session encrypted under one key cannot be read with another")
    void wrongKeyCannotDecrypt() {
        String encrypted = cipher(KEY_A).encrypt(PAYLOAD);

        assertThatThrownBy(() -> cipher(KEY_B).decrypt(encrypted, SessionPayload.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to decrypt workforce session");
    }

    @Test
    @DisplayName("a tampered ciphertext is rejected by the GCM authentication tag")
    void tamperedCiphertextIsRejected() {
        TokenVaultCipher cipher = cipher(KEY_A);
        byte[] raw = Base64.getUrlDecoder().decode(cipher.encrypt(PAYLOAD));

        // Flip a bit in the ciphertext body, past the 12-byte IV.
        raw[raw.length - 1] ^= 0x01;
        String tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        assertThatThrownBy(() -> cipher.decrypt(tampered, SessionPayload.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to decrypt workforce session");
    }

    @Test
    @DisplayName("a truncated value is rejected rather than partially decrypted")
    void truncatedValueIsRejected() {
        TokenVaultCipher cipher = cipher(KEY_A);

        assertThatThrownBy(() -> cipher.decrypt("c2hvcnQ", SessionPayload.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to decrypt workforce session");
    }

    @Test
    @DisplayName("a value that is not valid base64 is rejected without leaking a parser error")
    void malformedBase64IsRejected() {
        TokenVaultCipher cipher = cipher(KEY_A);

        assertThatThrownBy(() -> cipher.decrypt("!!!not-base64!!!", SessionPayload.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to decrypt workforce session");
    }

    @Test
    @DisplayName("a key that is not 32 bytes is refused at construction, not silently accepted")
    void shortKeyIsRefused() {
        String sixteenByteKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> cipher(sixteenByteKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must decode to 32 bytes");
    }

    @Test
    @DisplayName("an over-long key is also refused")
    void oversizedKeyIsRefused() {
        String sixtyFourByteKey = Base64.getEncoder().encodeToString(new byte[64]);

        assertThatThrownBy(() -> cipher(sixtyFourByteKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must decode to 32 bytes");
    }

    private TokenVaultCipher cipher(String encryptionKey) {
        return new TokenVaultCipher(properties(encryptionKey), objectMapper);
    }

    private static WorkforceSessionProperties properties(String encryptionKey) {
        return new WorkforceSessionProperties(
            "in-memory",
            "WF_SESSION",
            encryptionKey,
            Duration.ofMinutes(5),
            Duration.ofMinutes(1),
            Duration.ofHours(8),
            Set.of("https://bank.example/return"));
    }

    private static String base64Key(byte fill) {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, fill);
        return Base64.getEncoder().encodeToString(key);
    }
}
