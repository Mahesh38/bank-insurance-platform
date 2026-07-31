package com.bank.persistence.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("COMP-003")
class PayloadEncryptionPropertiesTest {

    @Test
    void acceptsValid32ByteKey_andDefaultsRetainYears() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        PayloadEncryptionProperties props = new PayloadEncryptionProperties("local-v1", key, null);

        assertThat(props.keyId()).isEqualTo("local-v1");
        assertThat(props.decodedKey()).hasSize(32);
        assertThat(props.retainYearsOrDefault()).isEqualTo(7);
    }

    @Test
    void failsFast_whenKeyWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new PayloadEncryptionProperties("bad", shortKey, 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void failsFast_whenKeyIdMissing() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);

        assertThatThrownBy(() -> new PayloadEncryptionProperties("  ", key, 7))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("key-id");
    }
}
