package com.bank.platform.proposal.support;

import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.Nominee;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.domain.UwRequirement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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

  public static Map<String, String> answers() {
    return Map.of("proposer.panNumber", PAN, "health.smoker", "NO");
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
