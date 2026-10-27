package com.bank.workforce.bff.lead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.config.BffClientConfig;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.config.DownstreamTimeouts;
import com.bank.workforce.bff.customer.HttpCustomerGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** A downstream that does not answer at all is a typed, retryable 503 — not a 500. */
@Tag("unit")
class DownstreamTransportFailureTest {

  private DownstreamProperties properties;
  private final ServiceErrors errors = ServiceErrors.of("workforce-access-bff", PlatformLayer.L4);
  private final ProblemJsonReader reader = new ProblemJsonReader(new ObjectMapper());

  @BeforeEach
  void pointAtAClosedPort() throws Exception {
    int port;
    try (ServerSocket socket = new ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    URI dead = URI.create("http://127.0.0.1:" + port);
    properties =
        new DownstreamProperties(
            URI.create("http://adapter.example.test"),
            "local-idp-adapter-internal-key",
            dead,
            dead,
            "local-lead-bff-internal-key",
            dead,
            "local-customer-bff-internal-key",
            "http",
            "http",
            "local-authz-bff-internal-key");
  }

  @Test
  void leadServiceDownIsUpstreamUnavailable() {
    var gateway = new HttpLeadGateway(RestClient.create(), properties, reader, errors);

    assertThatThrownBy(() -> gateway.get("L1", "rm-1"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              var se = (ServiceException) ex;
              assertThat(se.getErrorResponse().getCode())
                  .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
              assertThat(se.getHttpStatus()).isEqualTo(503);
            });
  }

  @Test
  void slowLeadServiceIsCutAtItsBudget() throws Exception {
    HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    slow.createContext(
        "/",
        exchange -> {
          try {
            Thread.sleep(2_000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    slow.start();
    try {
      URI base = URI.create("http://127.0.0.1:" + slow.getAddress().getPort());
      var props =
          new DownstreamProperties(
              URI.create("http://adapter.example.test"),
              "local-idp-adapter-internal-key",
              base,
              base,
              "local-lead-bff-internal-key",
              base,
              "local-customer-bff-internal-key",
              "http",
              "http",
              "local-authz-bff-internal-key");
      var budget = new DownstreamTimeouts.Budget(Duration.ofMillis(200), Duration.ofMillis(300));
      var gateway =
          new HttpLeadGateway(
              BffClientConfig.restClient(RestClient.builder(), budget), props, reader, errors);

      long started = System.nanoTime();
      assertThatThrownBy(() -> gateway.get("L1", "rm-1"))
          .isInstanceOf(ServiceException.class)
          .satisfies(
              ex ->
                  assertThat(((ServiceException) ex).getErrorResponse().getCode())
                      .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE));
      assertThat(Duration.ofNanos(System.nanoTime() - started))
          .isLessThan(Duration.ofMillis(1_500));
    } finally {
      slow.stop(0);
    }
  }

  @Test
  void customerServiceDownIsUpstreamUnavailable() {
    var gateway = new HttpCustomerGateway(RestClient.create(), properties, reader, errors);

    assertThatThrownBy(() -> gateway.search("MOBILE", "9876543210", "IN", "rm-1"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE));
  }
}
