package com.bank.common.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InternalCallerTest {

  private ServiceErrors errors;
  private InternalCallerCredentials credentials;

  @BeforeEach
  void setUp() {
    errors = ServiceErrors.of("test", PlatformLayer.L5);
    credentials =
        new InternalCallerCredentials(
            Map.of("bff", "local-authz-bff-internal-key"),
            Set.of("bff"),
            Map.of("bff", Set.of(InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE)));
  }

  @Test
  void requireCaller_returnsBoundCaller() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getAttribute(InternalCallerAuth.CALLER_ATTRIBUTE)).thenReturn("bff");
    assertThat(InternalCaller.requireCaller(request, errors, "op")).isEqualTo("bff");
  }

  @Test
  void requireCaller_rejectsMissingAttribute() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getAttribute(InternalCallerAuth.CALLER_ATTRIBUTE)).thenReturn(null);
    assertThatThrownBy(() -> InternalCaller.requireCaller(request, errors, "op"))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void requirePrivilege_allowsGrantedCaller() {
    InternalCaller.requirePrivilege(
        credentials, "bff", InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, errors, "op");
  }

  @Test
  void requirePrivilege_rejectsUngranted() {
    assertThatThrownBy(
            () ->
                InternalCaller.requirePrivilege(
                    credentials,
                    "bff",
                    InternalCallerAuth.PRIVILEGE_RAW_PAYLOAD_DECRYPT,
                    errors,
                    "op"))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void requireActorId_parsesUuidForActorCaller() {
    UUID id = UUID.randomUUID();
    assertThat(InternalCaller.requireActorId(credentials, "bff", id.toString(), errors, "op"))
        .isEqualTo(id);
    assertThat(InternalCaller.requireActorIdString(credentials, "bff", id.toString(), errors, "op"))
        .isEqualTo(id.toString());
  }

  @Test
  void requireActorId_rejectsNonActorCallerAndBadUuid() {
    assertThatThrownBy(
            () ->
                InternalCaller.requireActorId(
                    credentials, "lead", UUID.randomUUID().toString(), errors, "op"))
        .isInstanceOf(ServiceException.class);
    assertThatThrownBy(
            () -> InternalCaller.requireActorId(credentials, "bff", "not-a-uuid", errors, "op"))
        .isInstanceOf(ServiceException.class);
    assertThatThrownBy(() -> InternalCaller.requireActorId(credentials, "bff", " ", errors, "op"))
        .isInstanceOf(ServiceException.class);
  }
}
