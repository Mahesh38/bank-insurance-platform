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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock IdentityProviderClient provider;
    @Mock IdentityAuthorizationClient authorization;
    @Mock SessionStore sessions;

    private WorkforceSessionProperties properties;
    private LoginService service;

    @BeforeEach
    void setUp() {
        properties = new WorkforceSessionProperties("memory", "WORKFORCE_SESSION",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", Duration.ofMinutes(5), Duration.ofMinutes(1),
            Duration.ofHours(8), Set.of("https://app.example.test/auth/complete", "bankinsurance://auth-complete"));
        service = new LoginService(provider, authorization, sessions, properties);
    }

    @Test
    void beginsPkceLoginOnlyForAllowListedReturnUri() {
        URI authorizationUri = URI.create("https://idp.example.test/authorize");
        when(provider.authorizationUri(any())).thenReturn(authorizationUri);

        var result = service.begin(new LoginService.BeginLoginCommand(
            ClientType.WEB, IdentitySource.BANK_AD, "https://app.example.test/auth/complete", "rm@example.test"));

        assertThat(result.authorizationUri()).isEqualTo(authorizationUri);
        ArgumentCaptor<PendingLogin> pending = ArgumentCaptor.forClass(PendingLogin.class);
        verify(sessions).putPending(pending.capture(), any());
        assertThat(pending.getValue().codeVerifier()).hasSizeGreaterThan(40);
        assertThat(pending.getValue().identitySource()).isEqualTo(IdentitySource.BANK_AD);
    }

    @Test
    void rejectsOpenRedirectBeforeContactingProvider() {
        assertThatThrownBy(() -> service.begin(new LoginService.BeginLoginCommand(
            ClientType.WEB, IdentitySource.BANK_AD, "https://evil.example/steal", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not allowed");
        verify(provider, never()).authorizationUri(any());
    }

    @Test
    void completesWebLoginAndStoresProviderTokensOnlyInServerSession() {
        PendingLogin pending = pending(ClientType.WEB, IdentitySource.BANK_AD, "https://app.example.test/auth/complete");
        when(sessions.takePending("state")).thenReturn(Optional.of(pending));
        when(provider.exchange("code", "verifier", "nonce")).thenReturn(providerSession());
        when(authorization.resolve(eq(IdentitySource.BANK_AD), any())).thenReturn(activeIdentity());

        var result = service.complete("state", "code");

        assertThat(result.browserSessionId()).isNotBlank();
        assertThat(result.nativeCompletionCode()).isNull();
        ArgumentCaptor<WorkforceSession> session = ArgumentCaptor.forClass(WorkforceSession.class);
        verify(sessions).putSession(session.capture(), any());
        assertThat(session.getValue().accessToken()).isEqualTo("access-token");
        assertThat(session.getValue().businessUserId()).isEqualTo(activeIdentity().businessUserId());
    }

    @Test
    void nativeLoginUsesSingleUseCompletionCodeInsteadOfRedirectingSessionHandle() {
        PendingLogin pending = pending(ClientType.NATIVE, IdentitySource.PARTNER, "bankinsurance://auth-complete");
        when(sessions.takePending("state")).thenReturn(Optional.of(pending));
        when(provider.exchange("code", "verifier", "nonce")).thenReturn(providerSession());
        when(authorization.resolve(eq(IdentitySource.PARTNER), any())).thenReturn(activeIdentity());

        var result = service.complete("state", "code");

        assertThat(result.browserSessionId()).isNull();
        assertThat(result.nativeCompletionCode()).isNotBlank();
        verify(sessions).putCompletion(anyString(), any(NativeCompletion.class), any());
    }

    @Test
    void inactiveBusinessIdentityIsDeniedAndProviderSessionRevoked() {
        PendingLogin pending = pending(ClientType.WEB, IdentitySource.BANK_AD, "https://app.example.test/auth/complete");
        when(sessions.takePending("state")).thenReturn(Optional.of(pending));
        when(provider.exchange("code", "verifier", "nonce")).thenReturn(providerSession());
        when(authorization.resolve(eq(IdentitySource.BANK_AD), any()))
            .thenReturn(new IdentityAuthorizationClient.ResolvedIdentity(
                UUID.randomUUID(), "SUSPENDED", 4, "BANK_EMPLOYEE", null));

        assertThatThrownBy(() -> service.complete("state", "code"))
            .isInstanceOf(IllegalStateException.class);
        verify(provider).revoke("refresh-token");
        verify(sessions, never()).putSession(any(), any());
    }

    @Test
    void completionAndSessionHandlesAreSinglePurposeAndLogoutIsLocalFirst() {
        when(sessions.takeCompletion("completion"))
            .thenReturn(Optional.of(new NativeCompletion("session-id")));
        WorkforceSession session = workforceSession();
        when(sessions.getSession("session-id")).thenReturn(Optional.of(session));
        doThrow(new IllegalStateException("provider unavailable")).when(provider).revoke("refresh-token");

        assertThat(service.exchangeNativeCompletion("completion")).isEqualTo("session-id");
        assertThat(service.session("session-id")).isEqualTo(session);
        service.logout("session-id");

        verify(sessions).deleteSession("session-id");
        verify(provider).revoke("refresh-token");
    }

    private static PendingLogin pending(ClientType clientType, IdentitySource source, String returnUri) {
        return new PendingLogin("state", "nonce", "verifier", clientType, source, returnUri, Instant.now());
    }

    private static IdentityProviderClient.ProviderSession providerSession() {
        return new IdentityProviderClient.ProviderSession(
            "provider-subject", "user.demo", "user@example.test", "access-token", "refresh-token", "id-token",
            Instant.now().plusSeconds(300), Map.of("emp_id", "EMP-1"));
    }

    private static IdentityAuthorizationClient.ResolvedIdentity activeIdentity() {
        return new IdentityAuthorizationClient.ResolvedIdentity(
            UUID.fromString("39c1b574-df9e-43b4-bca8-38400f3d3410"), "ACTIVE", 3,
            "BANK_EMPLOYEE", null);
    }

    private static WorkforceSession workforceSession() {
        return new WorkforceSession("session-id", activeIdentity().businessUserId(), "BANK_EMPLOYEE", "ACTIVE", 3,
            null, "provider-subject", "user.demo", "access-token", "refresh-token", "id-token",
            Instant.now().plusSeconds(300), Instant.now(), Map.of());
    }
}
