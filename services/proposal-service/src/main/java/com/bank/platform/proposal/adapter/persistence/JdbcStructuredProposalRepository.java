package com.bank.platform.proposal.adapter.persistence;

import com.bank.common.events.Ulid;
import com.bank.platform.proposal.adapter.envelope.ProposalEnvelopeCodec;
import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.EnvelopeCipher;
import com.bank.platform.proposal.domain.MaterializationReceipt;
import com.bank.platform.proposal.domain.ProposalAnswer;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.SealedPayload;
import com.bank.platform.proposal.domain.StructuredProposalRepository;
import com.bank.platform.proposal.domain.UwRequirement;
import com.bank.platform.proposal.domain.WorkingProposal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Structured proposal on schema {@code proposal} (Flyway V2–V4, design DDL {@code
 * 09-proposal.sql}), written once per sale in ONE transaction: {@code proposal} (state {@code
 * PAID}), one {@code proposal_answer} per question (answer sealed per row), {@code uw_requirement},
 * {@code uw_document_ref}, the first {@code proposal_status_history} row {@code PAID} and the
 * receipt (D8).
 *
 * <p>{@code ON CONFLICT DO NOTHING} without a target is the form PostgreSQL and H2 both accept; a
 * re-delivered event inserts nothing (schemas/20 §3.4, §6 #1).
 */
@Repository
public class JdbcStructuredProposalRepository implements StructuredProposalRepository {

  static final String SYSTEM_ACTOR = "system:proposal";

  private final NamedParameterJdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final EnvelopeCipher cipher;
  private final ProposalEnvelopeCodec codec;
  private final Clock clock;
  private volatile String jsonBind;

  public JdbcStructuredProposalRepository(
      NamedParameterJdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      EnvelopeCipher cipher,
      ProposalEnvelopeCodec codec,
      Clock clock) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(transactions);
    this.cipher = cipher;
    this.codec = codec;
    this.clock = clock;
  }

  @Override
  public boolean exists(String proposalId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM proposal.proposal WHERE proposal_id = :id",
            new MapSqlParameterSource("id", proposalId),
            Integer.class);
    return count != null && count > 0;
  }

  @Override
  public void insert(WorkingProposal proposal, MaterializationReceipt receipt) {
    WorkingProposal.Snapshot s = proposal.snapshot();
    // INV-PRP-05: the structured rows hold the nominee and every answer encrypted, never in clear.
    SealedPayload nominee =
        cipher.seal(codec.encodeNominee(s.envelope().nominee()), nomineeContext(s.proposalId()));
    List<ProposalAnswer> answers = s.envelope().answers();
    List<SealedPayload> sealedAnswers =
        answers.stream()
            .map(
                a ->
                    cipher.seal(
                        (a.value() == null ? "" : a.value()).getBytes(StandardCharsets.UTF_8),
                        answerContext(s.proposalId(), a.questionId())))
            .toList();
    String counterOffer = codec.encodeCounterOffer(s.envelope().counterOffer());
    String json = jsonBind();
    transaction.executeWithoutResult(
        status -> {
          jdbc.update(
              "INSERT INTO proposal.proposal (proposal_id, lead_id, journey_id, quote_id,"
                  + " offer_id, consent_id, customer_id, application_number, schema_id,"
                  + " schema_version, state, issuance_mode, nominee_enc, encryption_key_id,"
                  + " underwriting_state, counter_offer, lob, config_version_ref, payment_id,"
                  + " materialized_event_id, materialized_at, created_at, updated_at, version)"
                  + " VALUES (:proposalId, :leadId, :journeyId, :quoteId, :offerId,"
                  + " :consentId, :customerId, :applicationNumber, :schemaId, :schemaVersion,"
                  + " :state, :issuanceMode, :nomineeEnc, :keyId, :underwritingState, "
                  + json.replace("?", ":counterOffer")
                  + ", :lob, :configVersionRef, :paymentId, :eventId, :materializedAt,"
                  + " :createdAt, :updatedAt, :version) ON CONFLICT DO NOTHING",
              new MapSqlParameterSource()
                  .addValue("proposalId", s.proposalId())
                  .addValue("leadId", s.leadId())
                  .addValue("journeyId", s.journeyId())
                  .addValue("quoteId", s.quoteId())
                  .addValue("offerId", s.offerId())
                  .addValue("consentId", s.consentId())
                  .addValue("customerId", s.customerId())
                  .addValue("applicationNumber", s.applicationNumber())
                  .addValue("schemaId", s.schemaId())
                  .addValue("schemaVersion", s.schemaVersion())
                  .addValue("state", s.state().name())
                  .addValue("issuanceMode", s.issuanceMode())
                  .addValue("nomineeEnc", nominee.ciphertext())
                  .addValue("keyId", nominee.encryptionKeyId())
                  .addValue("underwritingState", s.underwritingState())
                  .addValue("counterOffer", counterOffer)
                  .addValue("lob", s.lob())
                  .addValue("configVersionRef", s.configVersionRef())
                  .addValue("paymentId", receipt.paymentId())
                  .addValue("eventId", receipt.eventId())
                  .addValue("materializedAt", timestamp(s.materializedAt()))
                  .addValue("createdAt", timestamp(s.createdAt()))
                  .addValue("updatedAt", timestamp(s.updatedAt()))
                  .addValue("version", s.version()));
          for (int i = 0; i < answers.size(); i++) {
            ProposalAnswer a = answers.get(i);
            jdbc.update(
                "INSERT INTO proposal.proposal_answer (proposal_id, question_seq, question_id,"
                    + " section_id, question_text, schema_version, answer_enc,"
                    + " encryption_key_id, answered_at, answered_by, created_at) VALUES"
                    + " (:proposalId, :seq, :questionId, :sectionId, :questionText,"
                    + " :schemaVersion, :answerEnc, :keyId, :answeredAt, :answeredBy,"
                    + " :createdAt) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource()
                    .addValue("proposalId", s.proposalId())
                    .addValue("seq", i + 1)
                    .addValue("questionId", a.questionId())
                    .addValue("sectionId", a.sectionId())
                    .addValue("questionText", a.questionText())
                    .addValue("schemaVersion", a.schemaVersion())
                    .addValue("answerEnc", sealedAnswers.get(i).ciphertext())
                    .addValue("keyId", sealedAnswers.get(i).encryptionKeyId())
                    .addValue("answeredAt", timestamp(a.answeredAt()))
                    .addValue("answeredBy", a.answeredBy())
                    .addValue("createdAt", timestamp(s.updatedAt())));
          }
          for (UwRequirement r : s.envelope().requirements()) {
            jdbc.update(
                "INSERT INTO proposal.uw_requirement (requirement_id, proposal_id,"
                    + " requirement_type, sub_type, description, status, due_date, created_at)"
                    + " VALUES (:id, :proposalId, :type, :subType, :description, :status,"
                    + " :dueDate, :createdAt) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource()
                    .addValue("id", r.requirementId())
                    .addValue("proposalId", s.proposalId())
                    .addValue("type", r.requirementType())
                    .addValue("subType", r.subType())
                    .addValue("description", r.description())
                    .addValue("status", r.status())
                    .addValue("dueDate", r.dueDate() == null ? null : Date.valueOf(r.dueDate()))
                    .addValue("createdAt", timestamp(s.updatedAt())));
          }
          for (DocumentRef d : s.envelope().documents()) {
            jdbc.update(
                "INSERT INTO proposal.uw_document_ref (document_id, proposal_id, object_ref,"
                    + " document_class, created_at) VALUES (:id, :proposalId, :objectRef,"
                    + " :documentClass, :createdAt) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource()
                    .addValue("id", d.documentId())
                    .addValue("proposalId", s.proposalId())
                    .addValue("objectRef", d.objectRef())
                    .addValue("documentClass", d.documentClass())
                    .addValue("createdAt", timestamp(s.updatedAt())));
          }
          // Append-only history, first row PAID (D8). One PAID row per proposal whatever the
          // number of deliveries: guarded by NOT EXISTS, and by ux_prop_hist_event per event.
          jdbc.update(
              "INSERT INTO proposal.proposal_status_history (status_id, proposal_id,"
                  + " from_state, to_state, reason_code, source_event_id, actor_id, occurred_at,"
                  + " created_at) SELECT :statusId, :proposalId, :fromState, :toState, NULL,"
                  + " :eventId, :actorId, :occurredAt, :createdAt WHERE NOT EXISTS (SELECT 1"
                  + " FROM proposal.proposal_status_history WHERE proposal_id = :proposalId"
                  + " AND to_state = :toState) ON CONFLICT DO NOTHING",
              new MapSqlParameterSource()
                  .addValue("statusId", Ulid.mint(clock))
                  .addValue("proposalId", s.proposalId())
                  .addValue("fromState", ProposalState.AWAITING_PAYMENT.name())
                  .addValue("toState", ProposalState.PAID.name())
                  .addValue("eventId", receipt.eventId())
                  .addValue("actorId", SYSTEM_ACTOR)
                  .addValue("occurredAt", timestamp(s.materializedAt()))
                  .addValue("createdAt", Timestamp.from(clock.instant())));
          jdbc.update(
              "INSERT INTO proposal.idempotency_record (idempotency_key, request_hash,"
                  + " response_ref, created_at, expires_at) VALUES (:key, :hash, :ref, :now,"
                  + " :expiresAt) ON CONFLICT DO NOTHING",
              new MapSqlParameterSource()
                  .addValue("key", receipt.eventId())
                  .addValue("hash", receipt.requestHash())
                  .addValue("ref", s.proposalId())
                  .addValue("now", timestamp(s.updatedAt()))
                  .addValue("expiresAt", timestamp(receipt.expiresAt())));
        });
  }

  /**
   * How a JSON text parameter binds to {@code counter_offer}: PostgreSQL needs a cast to {@code
   * jsonb}; H2 (unit tests) needs {@code FORMAT JSON}, or it stores a JSON string literal.
   */
  private String jsonBind() {
    String bind = jsonBind;
    if (bind == null) {
      DataSource dataSource = jdbc.getJdbcTemplate().getDataSource();
      try (Connection c = dataSource.getConnection()) {
        String product = c.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        bind = product.contains("postgres") ? "CAST(? AS jsonb)" : "? FORMAT JSON";
      } catch (SQLException e) {
        throw new IllegalStateException("database product could not be read", e);
      }
      jsonBind = bind;
    }
    return bind;
  }

  static String nomineeContext(String proposalId) {
    return "proposal.proposal.nominee_enc|" + proposalId;
  }

  /** AAD binds each answer to its row (proposal LLD §4.6). */
  static String answerContext(String proposalId, String questionId) {
    return "proposal.proposal_answer|" + proposalId + "|" + questionId;
  }

  private static Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
