package com.bank.platform.proposal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.platform.proposal.domain.CounterOffer;
import com.bank.platform.proposal.domain.CreateOutcome;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.support.EmbeddedDynamoDbTestConfig;
import com.bank.platform.proposal.support.EventSchemas;
import com.bank.platform.proposal.support.LocalDynamoDb;
import com.bank.platform.proposal.support.ProposalFixtures;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** DynamoDB Local working item → H2 (PostgreSQL mode) structured rows, through the services. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EmbeddedDynamoDbTestConfig.class)
class ProposalMaterializationIntegrationTest {

  @Autowired private ProposalWorkingStateService proposals;
  @Autowired private ProposalMaterializer materializer;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MockMvc mvc;
  @Autowired private DynamoDbClient dynamo;

  private String awaitingPayment(String journeyId) {
    NewProposal request = ProposalFixtures.newProposal(journeyId);
    String id = proposals.create("journey:" + journeyId + ":proposal:1", request).proposalId();
    proposals.saveAnswers(id, ProposalFixtures.answers(), ProposalFixtures.nominee());
    proposals.submit(id);
    proposals.acknowledgeApplication(id, "APP-" + id);
    proposals.recordUnderwriting(
        id,
        ProposalState.REQUIREMENTS_PENDING,
        "REQUIREMENTS_PENDING",
        List.of(ProposalFixtures.requirement()),
        List.of(ProposalFixtures.document()));
    proposals.recordUnderwriting(id, ProposalState.UW_APPROVED, "APPROVED", null, null);
    proposals.awaitPayment(id);
    return id;
  }

  private int count(String sql, String id) {
    return jdbc.queryForObject(sql, Integer.class, id);
  }

  private static String code(Throwable e) {
    return ((ServiceException) e).getErrorResponse().getCode();
  }

  @Test
  void createReplaysAndValidatesKey() {
    NewProposal request = ProposalFixtures.newProposal();
    CreateOutcome first = proposals.create("k-" + request.journeyId(), request);

    assertThat(proposals.create("k-" + request.journeyId(), request).proposalId())
        .isEqualTo(first.proposalId());
    assertThatThrownBy(() -> proposals.create("", request))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.MISSING_IDEMPOTENCY_KEY));
    assertThatThrownBy(() -> proposals.get(Ulid.mint()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    assertThatThrownBy(() -> proposals.awaitPayment(first.proposalId()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
  }

  @Test
  void paymentInFlightIsIdempotentAndEndingRestoresExpiry() {
    String id = awaitingPayment(Ulid.mint());

    long version = proposals.markPaymentInFlight(id).version();
    assertThat(proposals.markPaymentInFlight(id).version()).isEqualTo(version);
    assertThat(proposals.paymentEnded(id).paymentInFlight()).isFalse();
  }

  @Test
  void materializeIsIdempotentAndFreezesTheItem() throws Exception {
    String journeyId = Ulid.mint();
    String id = awaitingPayment(journeyId);
    proposals.markPaymentInFlight(id);
    String paymentId = Ulid.mint();
    String body =
        "{\"eventId\":\""
            + Ulid.mint()
            + "\",\"proposalId\":\""
            + id
            + "\",\"paymentId\":\""
            + paymentId
            + "\",\"quoteId\":\""
            + Ulid.mint()
            + "\"}";

    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("MATERIALIZED"));
    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ALREADY_MATERIALIZED"));

    assertThat(count("SELECT COUNT(*) FROM proposal.proposal WHERE proposal_id = ?", id))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM proposal.uw_requirement WHERE proposal_id = ?", id))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM proposal.uw_document_ref WHERE proposal_id = ?", id))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM proposal.idempotency_record WHERE response_ref = ?", id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM proposal.proposal WHERE proposal_id = ?", String.class, id))
        .isEqualTo("PAID");
    assertThat(
            jdbc.queryForObject(
                "SELECT application_number FROM proposal.proposal WHERE proposal_id = ?",
                String.class,
                id))
        .isEqualTo("APP-" + id);
    // D8: one proposal_answer per question, text as shown, answer encrypted per row
    List<Map<String, Object>> answers =
        jdbc.queryForList(
            "SELECT question_seq, question_id, question_text, schema_version, answer_enc"
                + " FROM proposal.proposal_answer WHERE proposal_id = ? ORDER BY question_seq",
            id);
    assertThat(answers).hasSize(2);
    assertThat(answers.get(0))
        .containsEntry("QUESTION_ID", "health.smoker")
        .containsEntry("QUESTION_TEXT", "Question text shown for health.smoker")
        .containsEntry("SCHEMA_VERSION", ProposalFixtures.SCHEMA_VERSION);
    for (Map<String, Object> answer : answers) {
      assertThat(new String((byte[]) answer.get("ANSWER_ENC"), StandardCharsets.ISO_8859_1))
          .doesNotContain(ProposalFixtures.PAN);
    }
    // D8: append-only history, first row PAID, exactly once despite the re-delivery
    assertThat(
            jdbc.queryForList(
                "SELECT to_state FROM proposal.proposal_status_history WHERE proposal_id = ?",
                String.class,
                id))
        .containsExactly("PAID");
    assertThat(
            jdbc.queryForMap(
                "SELECT issuance_mode, payment_id, config_version_ref FROM proposal.proposal"
                    + " WHERE proposal_id = ?",
                id))
        .containsEntry("ISSUANCE_MODE", "NON_STP")
        .containsEntry("PAYMENT_ID", paymentId)
        .containsEntry("CONFIG_VERSION_REF", "cfg-2026-10-21");
    byte[] nominee =
        jdbc.queryForObject(
            "SELECT nominee_enc FROM proposal.proposal WHERE proposal_id = ?", byte[].class, id);
    assertThat(new String(nominee, StandardCharsets.ISO_8859_1))
        .doesNotContain(ProposalFixtures.NOMINEE_NAME);

    assertThat(proposals.get(id).isMaterialized()).isTrue();
    assertThatThrownBy(() -> proposals.paymentEnded(id))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
    proposals.create(
        "journey:" + journeyId + ":proposal:2", ProposalFixtures.newProposal(journeyId));
  }

  @Test
  void redeliveryRebuildsMissingStructuredRows() {
    String id = awaitingPayment(Ulid.mint());
    materializer.onPaymentReconciled(Ulid.mint(), id, Ulid.mint());
    jdbc.update("DELETE FROM proposal.uw_requirement WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.uw_document_ref WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.proposal_answer WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.proposal_status_history WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.proposal WHERE proposal_id = ?", id);

    assertThat(materializer.onPaymentReconciled(Ulid.mint(), id, Ulid.mint()))
        .isEqualTo(MaterializeOutcome.ALREADY_MATERIALIZED);
    assertThat(count("SELECT COUNT(*) FROM proposal.proposal WHERE proposal_id = ?", id))
        .isEqualTo(1);
  }

  @Test
  void onlyAProposalAwaitingPaymentMaterializes() {
    String draft = proposals.create(Ulid.mint(), ProposalFixtures.newProposal()).proposalId();

    assertThatThrownBy(() -> materializer.onPaymentReconciled(Ulid.mint(), draft, Ulid.mint()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
    assertThatThrownBy(
            () -> materializer.onPaymentReconciled(Ulid.mint(), Ulid.mint(), Ulid.mint()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    assertThat(proposals.withdraw(draft).state()).isEqualTo(ProposalState.WITHDRAWN);
  }

  private List<Map<String, AttributeValue>> outbox(String id) {
    return dynamo
        .query(
            r ->
                r.tableName(LocalDynamoDb.TABLE)
                    .keyConditionExpression("PK = :pk AND begins_with(SK, :sk)")
                    .expressionAttributeValues(
                        Map.of(
                            ":pk", AttributeValue.fromS("PROPOSAL#" + id),
                            ":sk", AttributeValue.fromS("OUTBOX#")))
                    .consistentRead(true))
        .items();
  }

  private org.springframework.test.web.servlet.ResultActions call(
      String path, String id, String body) throws Exception {
    return mvc.perform(
        post("/internal/v1/proposals/{id}/" + path, id)
            .header("X-Actor-Id", "system:payment")
            .header("Idempotency-Key", "payment:" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void paymentHoldNeedsAwaitingPaymentAndIsReleasedOnlyByItsPayment() throws Exception {
    String draft = proposals.create(Ulid.mint(), ProposalFixtures.newProposal()).proposalId();
    String paymentId = Ulid.mint();
    call("payment-hold", draft, "{\"paymentId\":\"" + paymentId + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PROPOSAL_NOT_PAYABLE));

    String id = awaitingPayment(Ulid.mint());
    for (int i = 0; i < 2; i++) {
      call("payment-hold", id, "{\"paymentId\":\"" + paymentId + "\"}")
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.applicationNumber").value("APP-" + id))
          .andExpect(jsonPath("$.issuanceMode").value("NON_STP"));
    }
    call("payment-hold", id, "{\"paymentId\":\"" + Ulid.mint() + "\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS));
    call(
            "payment-release",
            id,
            "{\"paymentId\":\"" + Ulid.mint() + "\",\"paymentState\":\"EXPIRED\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.released").value(false));
    call(
            "payment-release",
            id,
            "{\"paymentId\":\"" + paymentId + "\",\"paymentState\":\"DECLINED\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.released").value(true))
        .andExpect(jsonPath("$.state").value("AWAITING_PAYMENT"));
    mvc.perform(
            post("/internal/v1/proposals/{id}/payment-hold", id)
                .header("X-Actor-Id", "system:payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentId\":\"" + paymentId + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void counterOfferIsMaterialisedAndEveryOutboxPayloadMatchesTheContract() {
    String journeyId = Ulid.mint();
    String id =
        proposals.create("j:" + journeyId, ProposalFixtures.newProposal(journeyId)).proposalId();
    proposals.saveAnswers(id, ProposalFixtures.answers(), ProposalFixtures.nominee());
    proposals.submit(id);
    proposals.acknowledgeApplication(id, "APP-" + id);
    proposals.recordUnderwriting(
        id,
        ProposalState.UW_COUNTER_OFFER,
        "COUNTER_OFFER",
        null,
        null,
        new CounterOffer(
            Ulid.mint(), new BigDecimal("15500.00"), new BigDecimal("9000000"), "LOADING", null));
    proposals.decideCounterOffer(id, true);
    proposals.awaitPayment(id);
    materializer.onPaymentReconciled(Ulid.mint(), id, Ulid.mint());

    String counterOffer =
        jdbc.queryForObject(
            "SELECT CAST(counter_offer AS VARCHAR) FROM proposal.proposal WHERE proposal_id = ?",
            String.class,
            id);
    assertThat(counterOffer).contains("\"reasonCode\":\"LOADING\"").contains("15500.00");

    List<Map<String, AttributeValue>> items = outbox(id);
    assertThat(items)
        .extracting(i -> i.get("eventType").s())
        .contains(
            "ProposalCreated",
            "ProposalSubmitted",
            "ProposalAcknowledged",
            "UnderwritingDecision",
            "ProposalMaterialised");
    for (Map<String, AttributeValue> item : items) {
      assertThat(EventSchemas.validateEnvelope(item.get("envelope").s()))
          .as(item.get("eventType").s())
          .isEmpty();
      assertThat(item.get("published").bool()).isFalse();
      assertThat(item).doesNotContainKey("ttl");
    }
    String withdrawn = proposals.create(Ulid.mint(), ProposalFixtures.newProposal()).proposalId();
    proposals.withdraw(withdrawn);
    for (Map<String, AttributeValue> item : outbox(withdrawn)) {
      assertThat(EventSchemas.validateEnvelope(item.get("envelope").s())).isEmpty();
    }
  }

  @Test
  void invalidEventBodyIsRejected() throws Exception {
    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"e\"}"))
        .andExpect(status().is4xxClientError());
  }
}
