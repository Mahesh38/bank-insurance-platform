package com.bank.platform.suitability.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Ports and small values for the suitability journey. Adapters live outside the domain. */
public final class JourneyModel {

  private JourneyModel() {}

  public record FieldError(String id, String field, String message) {}

  public record RawAnswers(
      String policyFor,
      boolean lifePresent,
      String lifeName,
      String lifeDob,
      String lifeGender,
      String lifeMobile,
      String lifeEmail,
      String lifeStage,
      String risk,
      String goal,
      String occupation,
      String occupationOther,
      String education,
      String educationOther,
      String incomeBand) {}

  public record IncomeBand(String label, long floorInr) {}

  public record ProductMatch(
      String productId, String insurerCode, String name, String productType) {}

  public record DerivedPlan(
      long premiumInr,
      String frequency,
      String premiumPayingTerm,
      String policyTerm,
      String ruleId) {}

  public record LinkRequest(
      String customerId,
      String leadId,
      String journeyId,
      String assessmentId,
      String mobile,
      String email,
      String actorId) {}

  public record LinkReceipt(
      String state, String maskedMobile, String maskedEmail, Instant expiresAt, boolean sent) {}

  public interface Masters {
    boolean occupation(String value);

    boolean education(String value);

    boolean incomeBand(String label);

    Optional<IncomeBand> income(String label);
  }

  public interface ProductGrid {
    String version();

    List<ProductMatch> match(String productType, int age, RawAnswers answers);
  }

  public interface Assessments {
    void insert(Assessment assessment);

    void update(Assessment assessment);

    Optional<Assessment> lock(String id);

    boolean leadExists(String leadId);
  }

  public interface History {
    void answer(String assessmentId, String screen, String answers, String actorId, Instant at);

    void mapping(
        String assessmentId,
        int runNo,
        String version,
        String outcome,
        String actorId,
        Instant at,
        List<ProductMatch> products);
  }

  public interface FieldCipher {
    String keyId();

    byte[] encrypt(String plaintext);

    String decrypt(byte[] ciphertext);
  }

  public interface ConsentLinks {
    boolean holdsQuoteShare(String customerId);

    LinkReceipt issue(LinkRequest request);

    LinkReceipt resend(String assessmentId, String actorId);

    String linkState(String assessmentId);
  }
}
