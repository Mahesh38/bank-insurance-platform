package com.bank.common.events;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ADR-025 §2 naming. Domain topic {@code {env}.{context}.{aggregate}.v{n}} where context = envelope
 * {@code source} and aggregate = {@code aggregateType} in lower-kebab case, e.g. {@code
 * prod.payment.payment.v1}. {@code v{n}} is the topic major version — it changes only when the
 * envelope or keying changes; a breaking change to one event's data is a new {@code schemaVersion}
 * on the same topic (CATALOGUE.md "Versioning"). Retry and DLQ topics are per consumer group.
 */
public final class TopicResolver {

  private static final Pattern NAME = Pattern.compile("^[a-z][a-z0-9-]{0,47}$");

  private final String env;
  private final Map<String, Integer> topicVersions;

  public TopicResolver(String env) {
    this(env, Map.of());
  }

  /**
   * @param topicVersions overrides keyed by {@code context.aggregate} (e.g. {@code
   *     payment.payment}) for a topic that has moved to v2; everything else is v1
   */
  public TopicResolver(String env, Map<String, Integer> topicVersions) {
    this.env = requireName("env", env);
    this.topicVersions = Map.copyOf(topicVersions);
  }

  public String topicFor(EventEnvelope event) {
    return topicFor(event.source(), event.aggregateType());
  }

  public String topicFor(String context, String aggregateType) {
    String aggregate = aggregateType.toLowerCase(Locale.ROOT).replace('_', '-');
    String key = requireName("context", context) + "." + requireName("aggregate", aggregate);
    return env + "." + key + ".v" + topicVersions.getOrDefault(key, 1);
  }

  public String retryTopic(String consumerGroup) {
    return env + ".retry." + requireGroup(consumerGroup);
  }

  public String dlqTopic(String consumerGroup) {
    return env + ".dlq." + requireGroup(consumerGroup);
  }

  private static String requireGroup(String group) {
    if (group == null || !group.matches("^[a-z][a-z0-9.-]{0,95}$")) {
      throw new IllegalArgumentException("consumer group must be lower-case dotted: " + group);
    }
    return group;
  }

  private static String requireName(String what, String value) {
    if (value == null || !NAME.matcher(value).matches()) {
      throw new IllegalArgumentException(what + " is not a valid topic segment: " + value);
    }
    return value;
  }
}
