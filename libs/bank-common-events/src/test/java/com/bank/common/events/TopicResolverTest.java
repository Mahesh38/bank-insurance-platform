package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TopicResolverTest {

  @Test
  void followsTheAdr025Convention() {
    TopicResolver topics = new TopicResolver("prod");

    assertThat(topics.topicFor(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
        .isEqualTo("prod.payment.payment.v1");
    assertThat(topics.topicFor("suitability", "SUITABILITY_EVALUATION"))
        .isEqualTo("prod.suitability.suitability-evaluation.v1");
    assertThat(topics.retryTopic("proposal.materialiser"))
        .isEqualTo("prod.retry.proposal.materialiser");
    assertThat(topics.dlqTopic("audit.ingest")).isEqualTo("prod.dlq.audit.ingest");
  }

  @Test
  void topicVersionOverride() {
    TopicResolver topics = new TopicResolver("uat", Map.of("payment.payment", 2));

    assertThat(topics.topicFor("payment", "PAYMENT")).isEqualTo("uat.payment.payment.v2");
    assertThat(topics.topicFor("policy", "POLICY")).isEqualTo("uat.policy.policy.v1");
  }

  @Test
  void rejectsBadSegments() {
    assertThatThrownBy(() -> new TopicResolver("Prod"))
        .isInstanceOf(IllegalArgumentException.class);
    TopicResolver topics = new TopicResolver("dev");
    assertThatThrownBy(() -> topics.topicFor("pay.ment", "PAYMENT"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> topics.dlqTopic("Bad Group"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> topics.retryTopic(null)).isInstanceOf(IllegalArgumentException.class);
  }
}
