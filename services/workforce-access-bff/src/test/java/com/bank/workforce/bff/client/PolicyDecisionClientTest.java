package com.bank.workforce.bff.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@Tag("unit")
class PolicyDecisionClientTest {

  private HttpServer pdp;
  private final AtomicReference<String> answer = new AtomicReference<>();
  private final AtomicReference<String> lastRequest = new AtomicReference<>();
  private volatile long delayMillis;
  private PolicyDecisionClient client;

  @BeforeEach
  void startPdp() throws Exception {
    pdp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    pdp.createContext(
        "/internal/v1/authorization/decisions",
        exchange -> {
          lastRequest.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          try {
            Thread.sleep(delayMillis);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          byte[] body = answer.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    pdp.start();
    URI base = URI.create("http://127.0.0.1:" + pdp.getAddress().getPort());
    client =
        new PolicyDecisionClient(
            RestClient.builder(),
            new DownstreamProperties(
                URI.create("http://adapter.example.test"),
                "local-idp-adapter-internal-key",
                base,
                URI.create("http://lead.example.test"),
                URI.create("http://customer.example.test"),
                "stub",
                "stub",
                "local-authz-bff-internal-key"),
            ServiceErrors.of("workforce-access-bff", PlatformLayer.L4),
            Duration.ofMillis(300));
  }

  @AfterEach
  void stopPdp() {
    pdp.stop(0);
  }

  @Test
  void allowReturnsNormallyAndSendsTheSubject() {
    answer.set(decision(true, "ROLE_GRANT"));

    assertThatCode(() -> client.require(session(), "lead.create", "lead", null, "BR-1", false))
        .doesNotThrowAnyException();
    assertThat(lastRequest.get())
        .contains("\"subjectId\":\"11111111-1111-1111-1111-111111111111\"")
        .contains("\"action\":\"lead.create\"")
        .contains("\"branchCode\":\"BR-1\"");
  }

  @Test
  void denyBecomesTheMappedTypedError() {
    answer.set(decision(false, "BRANCH_OUT_OF_SCOPE"));

    assertThatThrownBy(() -> client.require(session(), "lead.create", "lead", null, "BR-9", false))
        .isInstanceOf(ServiceException.class)
        .satisfies(ex -> assertThat(code(ex)).isEqualTo(ErrorCodes.OUT_OF_BRANCH_SCOPE));
  }

  @Test
  void slowPdpFailsClosed() {
    answer.set(decision(true, "ROLE_GRANT"));
    delayMillis = 1_500;

    assertThatThrownBy(() -> client.require(session(), "lead.assign", "lead", "L1", null, false))
        .isInstanceOf(ServiceException.class)
        .satisfies(ex -> assertThat(code(ex)).isEqualTo(ErrorCodes.AUTHORIZATION_UNAVAILABLE));
  }

  @Test
  void unreachablePdpFailsClosed() {
    pdp.stop(0);

    assertThatThrownBy(() -> client.require(session(), "lead.assign", "lead", "L1", null, false))
        .isInstanceOf(ServiceException.class)
        .satisfies(ex -> assertThat(code(ex)).isEqualTo(ErrorCodes.AUTHORIZATION_UNAVAILABLE));
  }

  @Test
  void reasonCodesMapToCatalogueCodes() {
    assertThat(PolicyDecisionClient.errorCodeFor("ACCOUNT_NOT_ACTIVE"))
        .isEqualTo(ErrorCodes.ACCOUNT_SUSPENDED);
    assertThat(PolicyDecisionClient.errorCodeFor("CROSS_INSURER_ACCESS"))
        .isEqualTo(ErrorCodes.CROSS_INSURER_DENIED);
    assertThat(PolicyDecisionClient.errorCodeFor("RESOURCE_NOT_SHARED_WITH_PARTNER"))
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
    assertThat(PolicyDecisionClient.errorCodeFor("MANDATORY_CERTIFICATION_INVALID"))
        .isEqualTo(ErrorCodes.SP_CERTIFICATION_REQUIRED);
    assertThat(PolicyDecisionClient.errorCodeFor("UNKNOWN_SUBJECT"))
        .isEqualTo(ErrorCodes.DEFAULT_DENY);
  }

  private static String code(Throwable ex) {
    return ((ServiceException) ex).getErrorResponse().getCode();
  }

  private static String decision(boolean allowed, String reason) {
    return "{\"allowed\":"
        + allowed
        + ",\"reasonCode\":\""
        + reason
        + "\",\"matchedPolicy\":\"p\",\"policyVersion\":1}";
  }

  private static WorkforceSession session() {
    return new WorkforceSession(
        "s1",
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "BANK_EMPLOYEE",
        "ACTIVE",
        1L,
        null,
        "sub-1",
        "rm.one",
        "a",
        "r",
        "i",
        Instant.now().plusSeconds(60),
        Instant.now(),
        Map.of());
  }
}
