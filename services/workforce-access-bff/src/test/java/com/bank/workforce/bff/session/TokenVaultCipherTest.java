package com.bank.workforce.bff.session;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.IdentitySource;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenVaultCipherTest {

    @Test
    void encryptsAndAuthenticatesSessionPayload() {
        var properties = new WorkforceSessionProperties("memory", "WORKFORCE_SESSION",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", Duration.ofMinutes(5), Duration.ofMinutes(1),
            Duration.ofHours(8), Set.of("https://example.test/complete"));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        var cipher = new TokenVaultCipher(properties, objectMapper);
        var pending = new PendingLogin("state", "nonce", "verifier", ClientType.WEB, IdentitySource.BANK_AD,
            "https://example.test/complete", Instant.parse("2026-08-06T10:00:00Z"));

        String encrypted = cipher.encrypt(pending);

        assertThat(encrypted).doesNotContain("verifier");
        assertThat(cipher.decrypt(encrypted, PendingLogin.class)).isEqualTo(pending);
    }
}
