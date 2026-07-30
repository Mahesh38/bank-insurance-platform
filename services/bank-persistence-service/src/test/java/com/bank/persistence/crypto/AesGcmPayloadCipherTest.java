package com.bank.persistence.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("COMP-003")
class AesGcmPayloadCipherTest {

    private static final byte[] TEST_KEY = new byte[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
    };

    private AesGcmPayloadCipher cipher;

    @BeforeEach
    void setUp() {
        cipher = new AesGcmPayloadCipher(TEST_KEY);
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginalPlaintext() {
        byte[] plaintext = "secret-payload-COMP-003".getBytes(StandardCharsets.UTF_8);

        byte[] blob = cipher.encrypt(plaintext);
        byte[] decrypted = cipher.decrypt(blob);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encrypt_ciphertextDoesNotContainPlaintext() {
        byte[] plaintext = "UNIQUE_SECRET_TOKEN_XYZ".getBytes(StandardCharsets.UTF_8);

        byte[] blob = cipher.encrypt(plaintext);
        String blobAsUtf8 = new String(blob, StandardCharsets.UTF_8);

        assertThat(blobAsUtf8).doesNotContain("UNIQUE_SECRET_TOKEN_XYZ");
        assertThat(indexOf(blob, plaintext)).isLessThan(0);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime_dueToRandomIv() {
        byte[] plaintext = "same-input".getBytes(StandardCharsets.UTF_8);

        byte[] a = cipher.encrypt(plaintext);
        byte[] b = cipher.encrypt(plaintext);

        assertThat(a).isNotEqualTo(b);
        assertThat(cipher.decrypt(a)).isEqualTo(plaintext);
        assertThat(cipher.decrypt(b)).isEqualTo(plaintext);
    }

    @Test
    void decrypt_tamperedCiphertext_fails() {
        byte[] blob = cipher.encrypt("ok".getBytes(StandardCharsets.UTF_8));
        blob[blob.length - 1] ^= 0x01;

        assertThatThrownBy(() -> cipher.decrypt(blob))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("decrypt failed");
    }

    @Test
    void constructor_rejectsWrongKeyLength() {
        assertThatThrownBy(() -> new AesGcmPayloadCipher(new byte[16]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
