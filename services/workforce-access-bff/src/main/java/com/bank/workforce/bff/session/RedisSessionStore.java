package com.bank.workforce.bff.session;

import com.bank.workforce.bff.session.SessionModels.NativeCompletion;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "workforce.session.store", havingValue = "redis")
public class RedisSessionStore implements SessionStore {

    private final StringRedisTemplate redis;
    private final TokenVaultCipher cipher;

    public RedisSessionStore(StringRedisTemplate redis, TokenVaultCipher cipher) {
        this.redis = redis;
        this.cipher = cipher;
    }

    @Override
    public void putPending(PendingLogin pending, Duration ttl) {
        put("workforce:pending:" + pending.state(), pending, ttl);
    }

    @Override
    public Optional<PendingLogin> takePending(String state) {
        return take("workforce:pending:" + state, PendingLogin.class);
    }

    @Override
    public void putSession(WorkforceSession session, Duration ttl) {
        put("workforce:session:" + session.sessionId(), session, ttl);
    }

    @Override
    public Optional<WorkforceSession> getSession(String sessionId) {
        String encrypted = redis.opsForValue().get("workforce:session:" + sessionId);
        return Optional.ofNullable(encrypted).map(value -> cipher.decrypt(value, WorkforceSession.class));
    }

    @Override
    public void deleteSession(String sessionId) {
        redis.delete("workforce:session:" + sessionId);
    }

    @Override
    public void putCompletion(String code, NativeCompletion completion, Duration ttl) {
        put("workforce:completion:" + code, completion, ttl);
    }

    @Override
    public Optional<NativeCompletion> takeCompletion(String code) {
        return take("workforce:completion:" + code, NativeCompletion.class);
    }

    private void put(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, cipher.encrypt(value), ttl);
    }

    private <T> Optional<T> take(String key, Class<T> type) {
        String encrypted = redis.opsForValue().getAndDelete(key);
        return Optional.ofNullable(encrypted).map(value -> cipher.decrypt(value, type));
    }
}
