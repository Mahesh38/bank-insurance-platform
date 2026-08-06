package com.bank.workforce.bff.session;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import com.bank.workforce.bff.session.SessionModels.IdentitySource;
import com.bank.workforce.bff.session.SessionModels.NativeCompletion;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionStoreTest {

    private InMemorySessionStore store;

    @BeforeEach
    void setUp() {
        var properties = new WorkforceSessionProperties("memory", "WORKFORCE_SESSION",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", Duration.ofMinutes(5), Duration.ofMinutes(1),
            Duration.ofHours(8), Set.of("https://example.test/complete"));
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        store = new InMemorySessionStore(new TokenVaultCipher(properties, mapper));
    }

    @Test
    void pendingAndCompletionValuesAreConsumedOnce() {
        var pending = new PendingLogin("state", "nonce", "verifier", ClientType.WEB, IdentitySource.BANK_AD,
            "https://example.test/complete", Instant.now());
        store.putPending(pending, Duration.ofMinutes(1));
        store.putCompletion("completion", new NativeCompletion("session"), Duration.ofMinutes(1));

        assertThat(store.takePending("state")).contains(pending);
        assertThat(store.takePending("state")).isEmpty();
        assertThat(store.takeCompletion("completion")).contains(new NativeCompletion("session"));
        assertThat(store.takeCompletion("completion")).isEmpty();
    }

    @Test
    void storesAndDeletesEncryptedWorkforceSession() {
        var session = new WorkforceSession("session", UUID.randomUUID(), "BANK_EMPLOYEE", "ACTIVE", 1,
            null, "subject", "rm.demo", "access", "refresh", "id", Instant.now().plusSeconds(60),
            Instant.now(), Map.of());
        store.putSession(session, Duration.ofMinutes(1));

        assertThat(store.getSession("session")).contains(session);
        store.deleteSession("session");
        assertThat(store.getSession("session")).isEmpty();
    }

    @Test
    void expiredValueIsNotReturned() {
        var completion = new NativeCompletion("session");
        store.putCompletion("expired", completion, Duration.ofSeconds(-1));
        assertThat(store.takeCompletion("expired")).isEmpty();
    }
}
