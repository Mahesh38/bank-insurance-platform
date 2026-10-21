package com.bank.workforce.bff.lead;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.lead.LeadGateway.AssignLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.CreateLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.Meeting;
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

/** What the BFF puts on the wire to lead-service (S-20 Idempotency-Key, Screen 7 meeting). */
@Tag("unit")
class HttpLeadGatewayForwardingTest {

  private static final String LEAD_JSON =
      "{\"leadId\":\"01JQX4K7R8M2N3P4Q5S6T7V8W9\",\"state\":\"NEW\",\"exceptionHold\":false,"
          + "\"exceptionRequired\":false}";

  private HttpServer lead;
  private final AtomicReference<String> idempotencyHeader = new AtomicReference<>();
  private final AtomicReference<String> body = new AtomicReference<>();
  private HttpLeadGateway gateway;

  @BeforeEach
  void start() throws Exception {
    lead = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    lead.createContext(
        "/",
        exchange -> {
          idempotencyHeader.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
          body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] out = LEAD_JSON.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, out.length);
          exchange.getResponseBody().write(out);
          exchange.close();
        });
    lead.start();
    URI base = URI.create("http://127.0.0.1:" + lead.getAddress().getPort());
    gateway =
        new HttpLeadGateway(
            RestClient.create(),
            new DownstreamProperties(
                URI.create("http://adapter.example.test"),
                "local-idp-adapter-internal-key",
                base,
                base,
                base,
                "http",
                "http"),
            new ProblemJsonReader(new ObjectMapper()),
            ServiceErrors.of("workforce-access-bff", PlatformLayer.L4));
  }

  @AfterEach
  void stop() {
    lead.stop(0);
  }

  @Test
  void idempotencyKeyIsAHeaderNotABodyField() {
    gateway.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "u-1", "idem-123456"));

    assertThat(idempotencyHeader.get()).isEqualTo("idem-123456");
    assertThat(body.get()).doesNotContain("idempotencyKey").contains("\"customerId\":\"cust-1\"");
  }

  @Test
  void noKeyMeansNoHeader() {
    gateway.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "u-1"));

    assertThat(idempotencyHeader.get()).isNull();
  }

  @Test
  void noteAndMeetingAreForwardedOnAssign() {
    gateway.assign(
        new AssignLeadCommand(
            "L1",
            "SP-1001",
            null,
            "u-1",
            "call after six",
            new Meeting("IN_PERSON", "2026-10-22", "11:00", null)));

    assertThat(body.get())
        .contains("\"note\":\"call after six\"")
        .contains("\"type\":\"IN_PERSON\"")
        .contains("\"time\":\"11:00\"");
  }
}
