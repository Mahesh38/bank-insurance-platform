package com.bank.platform.proposal.adapter.persistence;

import com.bank.platform.proposal.adapter.envelope.ProposalEnvelopeCodec;
import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.EnvelopeCipher;
import com.bank.platform.proposal.domain.MaterializationReceipt;
import com.bank.platform.proposal.domain.SealedPayload;
import com.bank.platform.proposal.domain.StructuredProposalRepository;
import com.bank.platform.proposal.domain.UwRequirement;
import com.bank.platform.proposal.domain.WorkingProposal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Structured proposal on schema {@code proposal} (Flyway V2/V3), written once per sale.
 *
 * <p>{@code ON CONFLICT DO NOTHING} without a target is the form PostgreSQL and H2 both accept; a
 * re-delivered event inserts nothing (schemas/20 §3.4).
 */
@Repository
public class JdbcStructuredProposalRepository implements StructuredProposalRepository {

  private final NamedParameterJdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final EnvelopeCipher cipher;
  private final ProposalEnvelopeCodec codec;

  public JdbcStructuredProposalRepository(
      NamedParameterJdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      EnvelopeCipher cipher,
      ProposalEnvelopeCodec codec) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(transactions);
    this.cipher = cipher;
    this.codec = codec;
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
    // INV-PRP-05: the structured row holds the nominee encrypted, never in clear.
    SealedPayload nominee =
        cipher.seal(codec.encodeNominee(s.envelope().nominee()), nomineeContext(s.proposalId()));
    transaction.executeWithoutResult(
        status -> {
          jdbc.update(
              "INSERT INTO proposal.proposal (proposal_id, lead_id, journey_id, quote_id,"
                  + " offer_id, consent_id, customer_id, application_number, schema_id,"
                  + " schema_version, form_values_ref, state, nominee_enc, encryption_key_id,"
                  + " underwriting_state, lob, config_version_ref, created_at, updated_at,"
                  + " version) VALUES (:proposalId, :leadId, :journeyId, :quoteId, :offerId,"
                  + " :consentId, :customerId, :applicationNumber, :schemaId, :schemaVersion,"
                  + " :formValuesRef, :state, :nomineeEnc, :keyId, :underwritingState, :lob,"
                  + " :configVersionRef, :createdAt, :updatedAt, :version)"
                  + " ON CONFLICT DO NOTHING",
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
                  .addValue("formValuesRef", formValuesRef(s))
                  .addValue("state", s.state().name())
                  .addValue("nomineeEnc", nominee.ciphertext())
                  .addValue("keyId", nominee.encryptionKeyId())
                  .addValue("underwritingState", s.underwritingState())
                  .addValue("lob", s.lob())
                  .addValue("configVersionRef", s.configVersionRef())
                  .addValue("createdAt", timestamp(s.createdAt()))
                  .addValue("updatedAt", timestamp(s.updatedAt()))
                  .addValue("version", s.version()));
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
   * 09-proposal.sql keeps form values out of the row and points at them. Until the S3 evidence
   * store exists the reference names the frozen working item version that holds them.
   */
  static String formValuesRef(WorkingProposal.Snapshot s) {
    return "proposal-working:PROPOSAL#" + s.proposalId() + "/META@v" + s.version();
  }

  static String nomineeContext(String proposalId) {
    return "proposal.proposal.nominee_enc|" + proposalId;
  }

  private static Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
