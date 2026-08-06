package com.bank.workforce.bff.session;

import com.bank.workforce.bff.session.SessionModels.NativeCompletion;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;

import java.time.Duration;
import java.util.Optional;

public interface SessionStore {
    void putPending(PendingLogin pending, Duration ttl);
    Optional<PendingLogin> takePending(String state);
    void putSession(WorkforceSession session, Duration ttl);
    Optional<WorkforceSession> getSession(String sessionId);
    void deleteSession(String sessionId);
    void putCompletion(String code, NativeCompletion completion, Duration ttl);
    Optional<NativeCompletion> takeCompletion(String code);
}
