package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Ports the application needs. Adapters live outside the domain. */
public final class ConsentPorts {

  private ConsentPorts() {}

  public interface PolicySource {
    PolicySnapshot current();

    /** Outcome of {@code CONSENT.POLICY} for these inputs. Null input values stay null. */
    String evaluateOutcome(Map<String, String> inputs, Instant evaluatedAt);

    /** Drops a cached version after {@code RuleSetPublished}. */
    void invalidate();
  }

  public interface StatementSource {
    StatementDoc current(String purpose, String language);

    void setStatus(String purpose, String status);
  }

  public interface RegisteredContact {
    /** CBS mobile digits. Never taken from the request body. */
    String digits(String customerId);
  }

  public interface OtpVerifier {
    boolean matches(String otp, String hash);
  }

  public interface ChallengeStore {
    void insert(ChallengeRow row);

    Optional<ChallengeRow> find(String challengeId);

    void update(ChallengeRow row);

    List<ChallengeRow> stale(Instant otpExpiredBefore);
  }

  public interface ConsentStore {
    void insert(ConsentRow row);

    Optional<ConsentRow> head(String customerId, String purpose, String insurerCode);

    Optional<ConsentRow> headOf(String consentId);

    List<ConsentRow> chain(String consentId);

    List<String> chainIdsForCustomer(String customerId);

    List<String> chainIdsForJourney(String journeyId);

    List<ConsentRow> grantedPastValidity(Instant asOf);
  }

  public interface IssueLedger {
    int countSince(String customerId, String purpose, Instant since);

    void record(String customerId, String purpose, Instant at);
  }

  public interface Outbox {
    void append(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant at);
  }
}
