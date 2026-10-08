package com.bank.workforce.bff.application;

import com.bank.workforce.bff.client.IdentityAuthorizationClient;
import com.bank.workforce.bff.client.IdentityProviderClient;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.ClientType;
import com.bank.workforce.bff.session.SessionModels.IdentitySource;
import com.bank.workforce.bff.session.SessionModels.NativeCompletion;
import com.bank.workforce.bff.session.SessionModels.PendingLogin;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    private final IdentityProviderClient provider;
    private final IdentityAuthorizationClient authorization;
    private final SessionStore sessions;
    private final WorkforceSessionProperties properties;
    private final SecureRandom random = new SecureRandom();

    public LoginService(
        IdentityProviderClient provider,
        IdentityAuthorizationClient authorization,
        SessionStore sessions,
        WorkforceSessionProperties properties
    ) {
        this.provider = provider;
        this.authorization = authorization;
        this.sessions = sessions;
        this.properties = properties;
    }

    public BeginLoginResult begin(BeginLoginCommand command) {
        validateReturnUri(command.returnUri());
        String state = randomToken();
        String nonce = randomToken();
        String verifier = randomToken();
        String challenge = sha256Url(verifier);
        sessions.putPending(new PendingLogin(state, nonce, verifier, command.clientType(), command.identitySource(),
            command.returnUri(), Instant.now()), properties.pendingLoginTtl());
        URI authorizationUri = provider.authorizationUri(new IdentityProviderClient.AuthorizationUriRequest(
            state, nonce, challenge, command.loginHint(), command.identitySource().name()));
        return new BeginLoginResult(authorizationUri, properties.pendingLoginTtl().toSeconds());
    }

    /**
     * Bank RM credential login: adapter verifies the employee against AD (true/false for an
     * active directory account). The BFF never validates the password itself and never stores it.
     */
    public BankLoginResult authenticateBank(BankLoginCommand command) {
        if (command.employeeId() == null || command.employeeId().isBlank()) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        if (command.password() == null || command.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        var adResult = provider.verifyAd(command.employeeId().trim(), command.password());
        if (adResult == null || !adResult.accepted()) {
            throw new IllegalStateException("Directory did not accept the credentials");
        }
        String username = hasText(adResult.username()) ? adResult.username() : command.employeeId().trim();
        var providerSession = new IdentityProviderClient.ProviderSession(
            command.employeeId().trim(),
            username,
            adResult.email(),
            null,
            null,
            null,
            Instant.now().plus(properties.sessionTtl()),
            Map.of("employee_id", command.employeeId().trim(), "ad_verified", true)
        );
        var identity = authorization.resolve(IdentitySource.BANK_AD, providerSession);
        if (!"ACTIVE".equals(identity.status()) || !"BANK_EMPLOYEE".equals(identity.userType())) {
            throw new IllegalStateException("Business identity is not an active bank employee");
        }
        String sessionId = randomToken();
        var session = new WorkforceSession(
            sessionId,
            identity.businessUserId(),
            identity.userType(),
            identity.status(),
            identity.policyVersion(),
            identity.insurerCode(),
            providerSession.providerSubjectId(),
            providerSession.username(),
            null,
            null,
            null,
            providerSession.accessTokenExpiresAt(),
            Instant.now(),
            providerSession.claims()
        );
        sessions.putSession(session, properties.sessionTtl());
        log.info("Bank AD login created a workforce session");
        return new BankLoginResult(
            command.clientType(),
            true,
            identity.businessUserId(),
            identity.userType(),
            identity.status(),
            identity.policyVersion(),
            command.clientType() == ClientType.WEB ? sessionId : null,
            command.clientType() == ClientType.NATIVE ? sessionId : null,
            properties.sessionTtl().toSeconds()
        );
    }

    public CompletionResult complete(String state, String code) {
        PendingLogin pending = sessions.takePending(state)
            .orElseThrow(() -> new IllegalArgumentException("Login transaction is invalid or expired"));
        var providerSession = provider.exchange(code, pending.codeVerifier(), pending.nonce());
        var identity = authorization.resolve(pending.identitySource(), providerSession);
        if (!"ACTIVE".equals(identity.status())) {
            provider.revoke(providerSession.refreshToken());
            throw new IllegalStateException("Business identity is not active");
        }
        String sessionId = randomToken();
        var session = new WorkforceSession(
            sessionId,
            identity.businessUserId(),
            identity.userType(),
            identity.status(),
            identity.policyVersion(),
            identity.insurerCode(),
            providerSession.providerSubjectId(),
            providerSession.username(),
            providerSession.accessToken(),
            providerSession.refreshToken(),
            providerSession.idToken(),
            providerSession.accessTokenExpiresAt(),
            Instant.now(),
            providerSession.claims()
        );
        sessions.putSession(session, properties.sessionTtl());
        if (pending.clientType() == ClientType.NATIVE) {
            String completionCode = randomToken();
            sessions.putCompletion(completionCode, new NativeCompletion(sessionId), properties.completionCodeTtl());
            return new CompletionResult(pending.clientType(), pending.returnUri(), null, completionCode);
        }
        return new CompletionResult(pending.clientType(), pending.returnUri(), sessionId, null);
    }

    public String exchangeNativeCompletion(String completionCode) {
        return sessions.takeCompletion(completionCode)
            .orElseThrow(() -> new IllegalArgumentException("Completion code is invalid or expired"))
            .sessionId();
    }

    public WorkforceSession session(String sessionId) {
        return sessions.getSession(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session is invalid or expired"));
    }

    public void logout(String sessionId) {
        WorkforceSession session = sessions.getSession(sessionId).orElse(null);
        sessions.deleteSession(sessionId);
        if (session != null && session.refreshToken() != null) {
            try {
                provider.revoke(session.refreshToken());
            } catch (RuntimeException exception) {
                log.warn("Provider session revocation failed after local session deletion; subject={}",
                    session.providerSubjectId());
            }
        }
    }

    private void validateReturnUri(String returnUri) {
        if (!properties.allowedReturnUris().contains(returnUri)) {
            throw new IllegalArgumentException("Return URI is not allowed");
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Url(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public record BeginLoginCommand(
        ClientType clientType,
        IdentitySource identitySource,
        String returnUri,
        String loginHint
    ) {}

    public record BeginLoginResult(URI authorizationUri, long expiresInSeconds) {}

    public record CompletionResult(
        ClientType clientType,
        String returnUri,
        String browserSessionId,
        String nativeCompletionCode
    ) {}

    public record BankLoginCommand(
        ClientType clientType,
        String employeeId,
        String password
    ) {
        @Override
        public String toString() {
            return "BankLoginCommand[clientType=" + clientType + "]";
        }
    }

    public record BankLoginResult(
        ClientType clientType,
        boolean authenticated,
        UUID businessUserId,
        String userType,
        String status,
        long policyVersion,
        String browserSessionId,
        String nativeSessionHandle,
        long expiresInSeconds
    ) {}

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
