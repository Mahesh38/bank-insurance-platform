package com.bank.rules.client.http;

import com.bank.rules.client.RuleSetQuery;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetValidationException;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * {@link RuleSetResolver} over administration-config-service's internal API (ADR-024 section 5):
 * {@code GET /internal/v1/rule-sets:resolve} and {@code GET
 * /internal/v1/rule-sets/{id}/versions/{v}}. One attempt within the budget (default 300 ms, the
 * S-21 budget), no retry, fail closed: any timeout, error status, non-published version, invalid
 * document or hash mismatch is a {@link RuleSetUnresolvableException}.
 *
 * <p>Wrap in {@link com.bank.rules.client.CachingRuleSetResolver} for the L1 cache.
 */
public final class HttpRuleSetResolver implements RuleSetResolver {

  public static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(300);

  private static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  private final RestClient client;

  public HttpRuleSetResolver(RestClient.Builder builder, String baseUrl, Duration timeout) {
    this(builder, baseUrl, timeout, null);
  }

  /**
   * @param internalKey optional {@code X-Internal-Service-Key} for administration-config inbound
   *     auth; blank or null leaves the header unset (local classpath seeds do not need it)
   */
  public HttpRuleSetResolver(
      RestClient.Builder builder, String baseUrl, Duration timeout, String internalKey) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(timeout);
    RestClient.Builder configured = builder.clone().requestFactory(factory).baseUrl(baseUrl);
    if (internalKey != null && !internalKey.isBlank()) {
      configured = configured.defaultHeader(INTERNAL_KEY_HEADER, internalKey);
    }
    this.client = configured.build();
  }

  @Override
  public RuleSet resolve(RuleSetQuery q) {
    String what = "rule set for " + q.domain() + "/" + q.lob() + "/" + q.productClass();
    String body =
        get(
            what,
            uri ->
                uri.path("/internal/v1/rule-sets:resolve")
                    .queryParam("domain", q.domain())
                    .queryParamIfPresent("lob", Optional.ofNullable(q.lob()))
                    .queryParamIfPresent("productClass", Optional.ofNullable(q.productClass()))
                    .queryParam("asOf", q.asOf().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    .build());
    return toRuleSet(what, body);
  }

  @Override
  public RuleSet version(String ruleSetId, String version) {
    String what = "rule set " + ruleSetId + "@" + version;
    String body =
        get(
            what,
            uri -> uri.path("/internal/v1/rule-sets/{id}/versions/{v}").build(ruleSetId, version));
    return toRuleSet(what, body);
  }

  private String get(String what, Function<UriBuilder, URI> uri) {
    try {
      String body = client.get().uri(uri).retrieve().body(String.class);
      if (body == null || body.isBlank()) {
        throw new RuleSetUnresolvableException(what + ": empty response");
      }
      return body;
    } catch (RestClientException e) {
      throw new RuleSetUnresolvableException(
          what + ": configuration service did not answer (" + e.getClass().getSimpleName() + ")",
          e);
    }
  }

  private static RuleSet toRuleSet(String what, String body) {
    JsonNode envelope;
    try {
      envelope = Values.mapper().readTree(body);
    } catch (JsonProcessingException e) {
      throw new RuleSetUnresolvableException(what + ": response is not JSON", e);
    }
    if (!"PUBLISHED".equals(envelope.path("status").asText())) {
      throw new RuleSetUnresolvableException(what + ": version is not PUBLISHED");
    }
    RuleSet rs;
    try {
      rs = RuleSetParser.parse(envelope.path("document"));
    } catch (RuleSetValidationException e) {
      throw new RuleSetUnresolvableException(what + ": document failed validation", e);
    }
    if (!rs.documentSha256().equals(envelope.path("documentSha256").asText())) {
      throw new RuleSetUnresolvableException(what + ": documentSha256 mismatch");
    }
    return rs;
  }
}
