package com.bank.rules.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.rules.client.RuleSetQuery;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class HttpRuleSetResolverTest {

  private static final RuleSetQuery QUERY =
      new RuleSetQuery(
          "HUB_ROUTING", "LIFE", "TERM", OffsetDateTime.parse("2026-12-01T00:00:00+05:30"));

  private HttpServer server;
  private final AtomicReference<String> body = new AtomicReference<>();
  private final AtomicReference<Integer> status = new AtomicReference<>(200);
  private final AtomicReference<String> lastUri = new AtomicReference<>();
  private final AtomicReference<String> lastInternalKey = new AtomicReference<>();
  private volatile long delayMs;
  private HttpRuleSetResolver resolver;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        ex -> {
          lastUri.set(ex.getRequestURI().toString());
          lastInternalKey.set(ex.getRequestHeaders().getFirst("X-Internal-Service-Key"));
          try {
            Thread.sleep(delayMs);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          byte[] bytes =
              body.get() == null ? new byte[0] : body.get().getBytes(StandardCharsets.UTF_8);
          ex.sendResponseHeaders(status.get(), bytes.length == 0 ? -1 : bytes.length);
          if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
          }
          ex.close();
        });
    server.start();
    resolver =
        new HttpRuleSetResolver(
            RestClient.builder(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            Duration.ofSeconds(3));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private static ObjectNode envelope(String status) {
    String json =
        RuleSetSeeds.all().stream()
            .filter(s -> s.resource().endsWith("HUB.ROUTING.json"))
            .findFirst()
            .orElseThrow()
            .json();
    RuleSet rs = RuleSetParser.parse(json);
    ObjectNode env = Values.mapper().createObjectNode();
    env.put("status", status);
    env.put("documentSha256", rs.documentSha256());
    env.set("document", rs.document());
    return env;
  }

  @Test
  void resolvesAPublishedVersion() {
    body.set(envelope("PUBLISHED").toString());
    RuleSet rs = resolver.resolve(QUERY);
    assertThat(rs.ruleSetId()).isEqualTo("HUB.ROUTING");
    assertThat(lastUri.get())
        .startsWith(
            "/internal/v1/rule-sets:resolve?domain=HUB_ROUTING&lob=LIFE&productClass=TERM&asOf=");
    assertThat(lastInternalKey.get()).isNull();
    assertThat(resolver.version("HUB.ROUTING", "1.0.0").version()).isEqualTo("1.0.0");
    assertThat(lastUri.get()).isEqualTo("/internal/v1/rule-sets/HUB.ROUTING/versions/1.0.0");
  }

  @Test
  void sendsOptionalInternalKeyWhenConfigured() {
    body.set(envelope("PUBLISHED").toString());
    HttpRuleSetResolver withKey =
        new HttpRuleSetResolver(
            RestClient.builder(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            Duration.ofSeconds(3),
            "local-admin-config-lead-internal-key");
    withKey.resolve(QUERY);
    assertThat(lastInternalKey.get()).isEqualTo("local-admin-config-lead-internal-key");
  }

  @Test
  void failsClosedOnEveryKindOfBadAnswer() {
    body.set(envelope("DRAFT").toString());
    assertThatThrownBy(() -> resolver.resolve(QUERY))
        .isInstanceOf(RuleSetUnresolvableException.class)
        .hasMessageContaining("not PUBLISHED");

    ObjectNode tampered = envelope("PUBLISHED");
    tampered.put("documentSha256", "00");
    body.set(tampered.toString());
    assertThatThrownBy(() -> resolver.resolve(QUERY)).hasMessageContaining("mismatch");

    ObjectNode invalid = envelope("PUBLISHED");
    ((ObjectNode) invalid.get("document")).put("format", "nope");
    body.set(invalid.toString());
    assertThatThrownBy(() -> resolver.resolve(QUERY)).hasMessageContaining("failed validation");

    body.set("not json");
    assertThatThrownBy(() -> resolver.resolve(QUERY)).hasMessageContaining("not JSON");

    body.set(null);
    assertThatThrownBy(() -> resolver.resolve(QUERY)).hasMessageContaining("empty response");

    body.set("{}");
    status.set(503);
    assertThatThrownBy(() -> resolver.resolve(QUERY)).hasMessageContaining("did not answer");
  }

  @Test
  void failsClosedWhenTheBudgetIsExceeded() {
    body.set(envelope("PUBLISHED").toString());
    delayMs = 1500;
    HttpRuleSetResolver tight =
        new HttpRuleSetResolver(
            RestClient.builder(),
            "http://127.0.0.1:" + server.getAddress().getPort(),
            HttpRuleSetResolver.DEFAULT_TIMEOUT);
    assertThatThrownBy(() -> tight.resolve(QUERY)).isInstanceOf(RuleSetUnresolvableException.class);
  }
}
