package com.bank.workforce.bff.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** customer-service requires X-Actor-Id on every internal call (search audit). */
@Tag("unit")
class HttpCustomerGatewayForwardingTest {

  private HttpServer customer;
  private final AtomicReference<String> actor = new AtomicReference<>();
  private HttpCustomerGateway gateway;

  @BeforeEach
  void start() throws Exception {
    customer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    customer.createContext(
        "/",
        exchange -> {
          actor.set(exchange.getRequestHeaders().getFirst("X-Actor-Id"));
          byte[] out =
              (exchange.getRequestURI().getPath().endsWith("search")
                      ? "{\"items\":[]}"
                      : "{\"customerId\":\"C1\"}")
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          exchange.getResponseBody().write(out);
          exchange.close();
        });
    customer.start();
    URI base = URI.create("http://127.0.0.1:" + customer.getAddress().getPort());
    gateway =
        new HttpCustomerGateway(
            RestClient.create(),
            new DownstreamProperties(
                URI.create("http://adapter.example.test"),
                "local-idp-adapter-internal-key",
                base,
                base,
                base,
                "http",
                "http",
                "local-authz-internal-key"),
            new ProblemJsonReader(new ObjectMapper()),
            ServiceErrors.of("workforce-access-bff", PlatformLayer.L4));
  }

  @AfterEach
  void stop() {
    customer.stop(0);
  }

  @Test
  void searchAndGetSendTheActor() {
    gateway.search("MOBILE", "9876543210", "IN", "u-1");
    assertThat(actor.get()).isEqualTo("u-1");

    gateway.get("C1", "u-2");
    assertThat(actor.get()).isEqualTo("u-2");
  }
}
