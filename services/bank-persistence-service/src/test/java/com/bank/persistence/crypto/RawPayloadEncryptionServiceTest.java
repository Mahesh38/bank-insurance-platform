package com.bank.persistence.crypto;

import com.bank.persistence.config.RawPayloadEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPayloadEncryptionServiceTest {

    private static final String VALID_KEY = "p7MSmdyOLeyBue5/ZJpPP3TE/ZQrLr4WSPjrhS0d0F8=";

    @Test
    void encryptThenDecrypt_roundTripsPlaintext() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));

        byte[] encrypted = service.encrypt("{\"applicationNo\":\"APP-1\",\"pan\":\"ABCDE1234F\"}");
        String decrypted = service.decrypt(encrypted);

        assertThat(decrypted).isEqualTo("{\"applicationNo\":\"APP-1\",\"pan\":\"ABCDE1234F\"}");
    }

    @Test
    void encrypt_neverLeaksPlaintextBytesInOutput() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));
        String plaintext = "super-secret-raw-payload-body";

        byte[] encrypted = service.encrypt(plaintext);

        assertThat(new String(encrypted)).doesNotContain(plaintext);
    }

    @Test
    void encrypt_isNonDeterministic_freshIvEachCall() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));

        byte[] first = service.encrypt("same-input");
        byte[] second = service.encrypt("same-input");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void keyId_isStableConstant() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));

        assertThat(service.keyId()).isEqualTo(RawPayloadEncryptionService.KEY_ID);
    }

    @Test
    void missingKey_failsFastAtConstruction() {
        assertThatThrownBy(() -> new RawPayloadEncryptionService(new RawPayloadEncryptionProperties(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RAW_PAYLOAD_ENCRYPTION_KEY");
    }

    @Test
    void blankKey_failsFastAtConstruction() {
        assertThatThrownBy(() -> new RawPayloadEncryptionService(new RawPayloadEncryptionProperties("  ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RAW_PAYLOAD_ENCRYPTION_KEY");
    }

    @Test
    void invalidBase64Key_failsFastAtConstruction() {
        assertThatThrownBy(() -> new RawPayloadEncryptionService(new RawPayloadEncryptionProperties("not-base64!!")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base64");
    }

    @Test
    void wrongLengthKey_failsFastAtConstruction() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new RawPayloadEncryptionService(new RawPayloadEncryptionProperties(shortKey)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void decrypt_tooShortInput_throws() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));

        assertThatThrownBy(() -> service.decrypt(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrypt_tamperedCiphertext_throws() {
        RawPayloadEncryptionService service = new RawPayloadEncryptionService(
                new RawPayloadEncryptionProperties(VALID_KEY));
        byte[] encrypted = service.encrypt("integrity-check");
        encrypted[encrypted.length - 1] ^= 0x01;

        assertThatThrownBy(() -> service.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class);
    }
}
