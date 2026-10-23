package com.bank.platform.proposal.support;

import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.Nominee;
import com.bank.platform.proposal.domain.ProposalAnswer;
import com.bank.platform.proposal.domain.ProposalEnvelope;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.domain.UwRequirement;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ProposalFixtures {

  public static final String NOMINEE_NAME = "Asha Ramesh Kulkarni";
  public static final String PAN = "ABCPK1234Q";

  private ProposalFixtures() {}

  public static NewProposal newProposal() {
    return newProposal(Ulid.mint());
  }

  public static NewProposal newProposal(String journeyId) {
    return new NewProposal(
        Ulid.mint(),
        journeyId,
        Ulid.mint(),
        Ulid.mint(),
        Ulid.mint(),
        Ulid.mint(),
        "LIFE",
        "INS01-TERM-FORM",
        "3",
        "NON_STP",
        "cfg-2026-10-21");
  }

  public static Nominee nominee() {
    return new Nominee(NOMINEE_NAME, LocalDate.of(1992, 1, 5), "SPOUSE", new BigDecimal("100"));
  }

  public static final String SCHEMA_VERSION = "3";

  public static List<ProposalAnswer> answers() {
    return answers(Map.of("proposer.panNumber", PAN, "health.smoker", "NO"));
  }

  /** Answers as the form showed them: question text and schema version recorded (D8). */
  public static List<ProposalAnswer> answers(Map<String, String> values) {
    List<ProposalAnswer> answers = new ArrayList<>();
    new TreeMap<>(values)
        .forEach(
            (id, value) ->
                answers.add(
                    new ProposalAnswer(
                        id,
                        "SEC-1",
                        "Question text shown for " + id,
                        SCHEMA_VERSION,
                        value,
                        Instant.parse("2026-10-21T09:00:00Z"),
                        "RM001")));
    return answers;
  }

  /** {@code questionId → value} of an envelope, for assertions. */
  public static Map<String, String> values(ProposalEnvelope envelope) {
    Map<String, String> values = new LinkedHashMap<>();
    envelope.answers().forEach(a -> values.put(a.questionId(), a.value()));
    return values;
  }

  public static UwRequirement requirement() {
    return new UwRequirement(
        Ulid.mint(),
        "DOCUMENT",
        "INCOME_PROOF",
        "Last 3 months salary slips",
        "PENDING",
        LocalDate.of(2026, 10, 28));
  }

  public static DocumentRef document() {
    return new DocumentRef(Ulid.mint(), "s3://proposal-docs/" + Ulid.mint(), "INCOME_PROOF");
  }
}
