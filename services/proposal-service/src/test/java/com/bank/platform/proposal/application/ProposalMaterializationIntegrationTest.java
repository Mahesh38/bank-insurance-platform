package com.bank.platform.proposal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.platform.proposal.domain.CreateOutcome;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.support.EmbeddedDynamoDbTestConfig;
import com.bank.platform.proposal.support.ProposalFixtures;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    String body =
        "{\"eventId\":\"evt-" + id + "\",\"proposalId\":\"" + id + "\",\"paymentId\":\"P1\"}";

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
    materializer.onPaymentReconciled("evt-a-" + id, id);
    jdbc.update("DELETE FROM proposal.uw_requirement WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.uw_document_ref WHERE proposal_id = ?", id);
    jdbc.update("DELETE FROM proposal.proposal WHERE proposal_id = ?", id);

    assertThat(materializer.onPaymentReconciled("evt-b-" + id, id))
        .isEqualTo(MaterializeOutcome.ALREADY_MATERIALIZED);
    assertThat(count("SELECT COUNT(*) FROM proposal.proposal WHERE proposal_id = ?", id))
        .isEqualTo(1);
  }

  @Test
  void onlyAProposalAwaitingPaymentMaterializes() {
    String draft = proposals.create(Ulid.mint(), ProposalFixtures.newProposal()).proposalId();

    assertThatThrownBy(() -> materializer.onPaymentReconciled("e1", draft))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.ILLEGAL_TRANSITION));
    assertThatThrownBy(() -> materializer.onPaymentReconciled("e2", Ulid.mint()))
        .satisfies(e -> assertThat(code(e)).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    assertThat(proposals.withdraw(draft).state()).isEqualTo(ProposalState.WITHDRAWN);
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
