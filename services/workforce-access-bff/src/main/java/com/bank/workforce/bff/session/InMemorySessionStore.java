package com.bank.workforce.bff.session;

import com.bank.workforce.bff.session.SessionModels.NativeCompletion;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "workforce.session.store", havingValue = "memory", matchIfMissing = true)
public class InMemorySessionStore implements SessionStore {

    private final TokenVaultCipher cipher;
    private final ConcurrentHashMap<String, ExpiringValue> values = new ConcurrentHashMap<>();

    public InMemorySessionStore(TokenVaultCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void putPending(PendingLogin pending, Duration ttl) {
        put("pending:" + pending.state(), pending, ttl);
    }

    @Override
    public Optional<PendingLogin> takePending(String state) {
        return take("pending:" + state, PendingLogin.class);
    }

    @Override
    public void putSession(WorkforceSession session, Duration ttl) {
        put("session:" + session.sessionId(), session, ttl);
    }

    @Override
    public Optional<WorkforceSession> getSession(String sessionId) {
        ExpiringValue value = values.get("session:" + sessionId);
        if (value == null || expired(value)) {
            values.remove("session:" + sessionId);
            return Optional.empty();
        }
        return Optional.of(cipher.decrypt(value.encrypted(), WorkforceSession.class));
    }

    @Override
    public void deleteSession(String sessionId) {
        values.remove("session:" + sessionId);
    }

    @Override
    public void putCompletion(String code, NativeCompletion completion, Duration ttl) {
        put("completion:" + code, completion, ttl);
    }

    @Override
    public Optional<NativeCompletion> takeCompletion(String code) {
        return take("completion:" + code, NativeCompletion.class);
    }

    private void put(String key, Object value, Duration ttl) {
        values.put(key, new ExpiringValue(cipher.encrypt(value), Instant.now().plus(ttl)));
    }

    private <T> Optional<T> take(String key, Class<T> type) {
        ExpiringValue value = values.remove(key);
        if (value == null || expired(value)) {
            return Optional.empty();
        }
        return Optional.of(cipher.decrypt(value.encrypted(), type));
    }

    private static boolean expired(ExpiringValue value) {
        return !Instant.now().isBefore(value.expiresAt());
    }

    private record ExpiringValue(String encrypted, Instant expiresAt) {}
}
