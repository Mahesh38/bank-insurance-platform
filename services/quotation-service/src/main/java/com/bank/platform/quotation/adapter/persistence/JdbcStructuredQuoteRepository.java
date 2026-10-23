package com.bank.platform.quotation.adapter.persistence;

import com.bank.platform.quotation.adapter.envelope.QuoteEnvelopeCodec;
import com.bank.platform.quotation.domain.EnvelopeCipher;
import com.bank.platform.quotation.domain.MaterializationReceipt;
import com.bank.platform.quotation.domain.OfferState;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.SealedPayload;
import com.bank.platform.quotation.domain.StructuredQuoteRepository;
import com.bank.platform.quotation.domain.WorkingQuote;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Structured quote on schema {@code quotation} (Flyway V1–V3, design DDL {@code 08-quotation.sql}),
 * written once per sale. Every offer shown is kept: the selected one {@code SELECTED}, other priced
 * ones {@code NOT_SELECTED}, {@code INVALID} ones with whatever the insurer returned (D8).
 *
 * <p>{@code ON CONFLICT DO NOTHING} without a target is the form PostgreSQL and H2 both accept; on
 * these tables the only unique keys are the primary keys and {@code ux_offer_selected}, which the
 * domain already guarantees (INV-QUO-05), so a re-delivered event is a no-op (schemas/20 §3.4).
 */
@Repository
public class JdbcStructuredQuoteRepository implements StructuredQuoteRepository {

  private final NamedParameterJdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final EnvelopeCipher cipher;
  private final QuoteEnvelopeCodec codec;

  public JdbcStructuredQuoteRepository(
      NamedParameterJdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      EnvelopeCipher cipher,
      QuoteEnvelopeCodec codec) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(transactions);
    this.cipher = cipher;
    this.codec = codec;
  }

  @Override
  public boolean exists(String quoteId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM quotation.quote WHERE quote_id = :quoteId",
            new MapSqlParameterSource("quoteId", quoteId),
            Integer.class);
    return count != null && count > 0;
  }

  @Override
  public void insert(WorkingQuote quote, MaterializationReceipt receipt) {
    WorkingQuote.Snapshot s = quote.snapshot();
    // members_enc is sealed separately so the structured row never holds plaintext PII.
    SealedPayload members =
        cipher.seal(codec.encodeMembers(s.envelope().members()), membersContext(s.quoteId()));
    transaction.executeWithoutResult(
        status -> {
          jdbc.update(
              "INSERT INTO quotation.quote (quote_id, lead_id, journey_id, customer_id,"
                  + " suitability_id, evaluation_id, consent_id, product_class, lob, mode,"
                  + " category, requested_cover, members_enc, encryption_key_id, state,"
                  + " selected_offer_id, valid_until, provider, provider_job_id,"
                  + " config_version_ref, materialized_event_id, materialized_at, created_at,"
                  + " updated_at, version) VALUES (:quoteId, :leadId, :journeyId, :customerId,"
                  + " :suitabilityId, :evaluationId, :consentId, :productClass, :lob, :mode,"
                  + " :category, :requestedCover, :membersEnc, :keyId, :state,"
                  + " :selectedOfferId, :validUntil, :provider, :providerJobId,"
                  + " :configVersionRef, :eventId, :materializedAt, :createdAt, :updatedAt,"
                  + " :version) ON CONFLICT DO NOTHING",
              new MapSqlParameterSource()
                  .addValue("quoteId", s.quoteId())
                  .addValue("leadId", s.leadId())
                  .addValue("journeyId", s.journeyId())
                  .addValue("customerId", s.customerId())
                  .addValue("suitabilityId", s.suitabilityId())
                  // Contract: suitabilityId carries the evaluation id the C1 gate used (D4).
                  .addValue("evaluationId", s.suitabilityId())
                  .addValue("productClass", s.productClass())
                  .addValue("selectedOfferId", s.envelope().selectedOfferId())
                  .addValue("eventId", receipt.eventId())
                  .addValue("materializedAt", timestamp(s.materializedAt()))
                  .addValue("consentId", s.consentId())
                  .addValue("lob", s.lob())
                  .addValue("mode", s.mode())
                  .addValue("category", s.category())
                  .addValue("requestedCover", s.envelope().requestedCover())
                  .addValue("membersEnc", members.ciphertext())
                  .addValue("keyId", members.encryptionKeyId())
                  .addValue("state", s.state().name())
                  .addValue("validUntil", timestamp(s.validUntil()))
                  .addValue("provider", s.provider())
                  .addValue("providerJobId", s.providerJobId())
                  .addValue("configVersionRef", s.configVersionRef())
                  .addValue("createdAt", timestamp(s.createdAt()))
                  .addValue("updatedAt", timestamp(s.updatedAt()))
                  .addValue("version", s.version()));
          for (QuoteOffer offer : s.envelope().offers()) {
            insertOffer(s, offer);
          }
          jdbc.update(
              "INSERT INTO quotation.idempotency_record (idempotency_key, request_hash,"
                  + " response_ref, created_at, expires_at) VALUES (:key, :hash, :ref, :now,"
                  + " :expiresAt) ON CONFLICT DO NOTHING",
              new MapSqlParameterSource()
                  .addValue("key", receipt.eventId())
                  .addValue("hash", receipt.requestHash())
                  .addValue("ref", s.quoteId())
                  .addValue("now", timestamp(s.updatedAt()))
                  .addValue("expiresAt", timestamp(receipt.expiresAt())));
        });
  }

  private void insertOffer(WorkingQuote.Snapshot s, QuoteOffer offer) {
    jdbc.update(
        "INSERT INTO quotation.offer (offer_id, quote_id, insurer_code, product_code,"
            + " product_name, premium_amount, premium_frequency, tax_amount, sum_assured,"
            + " cover_term, premium_paying_term, out_of_bound, offer_state, error_summary,"
            + " created_at) VALUES (:offerId, :quoteId, :insurerCode, :productCode,"
            + " :productName, :premium, :frequency, :tax, :sumAssured, :coverTerm, :ppt,"
            + " :outOfBound, :offerState, :errorSummary, :createdAt) ON CONFLICT DO NOTHING",
        new MapSqlParameterSource()
            .addValue("offerId", offer.offerId())
            .addValue("quoteId", s.quoteId())
            .addValue("insurerCode", offer.insurerCode())
            .addValue("productCode", offer.productCode())
            .addValue("productName", offer.productName())
            .addValue("premium", offer.premiumAmount())
            .addValue("frequency", offer.frequency())
            .addValue("tax", offer.taxAmount() == null ? BigDecimal.ZERO : offer.taxAmount())
            .addValue("sumAssured", offer.sumAssured())
            .addValue("coverTerm", offer.coverTerm())
            .addValue("ppt", offer.ppt())
            .addValue("outOfBound", offer.outOfBound())
            .addValue("offerState", structuredState(s, offer).name())
            .addValue("errorSummary", offer.errorSummary())
            .addValue("createdAt", timestamp(s.updatedAt())));
  }

  // schemas/20 §3.4: the selected offer is SELECTED, every other priced offer NOT_SELECTED.
  private static OfferState structuredState(WorkingQuote.Snapshot s, QuoteOffer offer) {
    if (offer.offerState() == OfferState.INVALID) {
      return OfferState.INVALID;
    }
    return offer.offerId().equals(s.envelope().selectedOfferId())
        ? OfferState.SELECTED
        : OfferState.NOT_SELECTED;
  }

  static String membersContext(String quoteId) {
    return "quotation.quote.members_enc|" + quoteId;
  }

  private static Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }
}
