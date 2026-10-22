package com.bank.platform.lead.adapter.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@Tag("unit")
class PdpLeadAuthorizationAdapterTest {

  private static final String SUBJECT = "11111111-1111-1111-1111-111111111111";

  private HttpServer pdp;
  private final AtomicReference<String> answer = new AtomicReference<>();
  private final AtomicReference<String> lastRequest = new AtomicReference<>();
  private final AtomicReference<String> lastKey = new AtomicReference<>();
  private volatile long delayMillis;
  private PdpLeadAuthorizationAdapter adapter;

  @BeforeEach
  void startPdp() throws Exception {
    pdp = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    pdp.createContext(
        "/internal/v1/authorization/decisions",
        exchange -> {
          lastKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Service-Key"));
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
    adapter =
        new PdpLeadAuthorizationAdapter(
            RestClient.builder(),
            "http://127.0.0.1:" + pdp.getAddress().getPort(),
            Duration.ofMillis(300),
            "lead-test-internal-key");
  }

  @AfterEach
  void stopPdp() {
    pdp.stop(0);
  }

  @Test
  void allowAndDenyAreRelayed() {
    answer.set(decision(true, "ROLE_GRANT"));
    assertThat(adapter.decide(SUBJECT, "lead.assign", "L1", "BR-1").allowed()).isTrue();
    assertThat(lastRequest.get()).contains("\"action\":\"lead.assign\"").contains("\"id\":\"L1\"");

    answer.set(decision(false, "BRANCH_OUT_OF_SCOPE"));
    var denied = adapter.decide(SUBJECT, "lead.create", null, "BR-9");
    assertThat(denied.allowed()).isFalse();
    assertThat(denied.unavailable()).isFalse();
    assertThat(denied.reasonCode()).isEqualTo("BRANCH_OUT_OF_SCOPE");
  }

  @Test
  void exceptionApprovalIsBranchScopedAndNeverPartnerVisible() {
    answer.set(decision(true, "ROLE_GRANT"));
    adapter.decide(SUBJECT, "lead.exception.approve", "L1", "BR-7");

    assertThat(lastRequest.get())
        .contains("\"action\":\"lead.exception.approve\"")
        .contains("\"branchCode\":\"BR-7\"")
        .contains("\"partnerVisible\":false");
  }

  @Test
  void presentsTheServiceKeyToThePdp() {
    answer.set(decision(true, "ROLE_GRANT"));
    adapter.decide(SUBJECT, "lead.read", "L1", null);

    assertThat(lastKey.get()).isEqualTo("lead-test-internal-key");
  }

  @Test
  void slowPdpIsUnavailableNotAllow() {
    answer.set(decision(true, "ROLE_GRANT"));
    delayMillis = 1_500;

    var decision = adapter.decide(SUBJECT, "lead.create", null, "BR-1");
    assertThat(decision.allowed()).isFalse();
    assertThat(decision.unavailable()).isTrue();
  }

  @Test
  void nonUuidPrincipalIsDeniedWithoutACall() {
    var decision = adapter.decide("rm.username", "lead.create", null, "BR-1");

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("UNKNOWN_SUBJECT");
    assertThat(lastRequest.get()).isNull();
  }

  private static String decision(boolean allowed, String reason) {
    return "{\"allowed\":"
        + allowed
        + ",\"reasonCode\":\""
        + reason
        + "\",\"matchedPolicy\":\"p\",\"policyVersion\":1}";
  }
}
