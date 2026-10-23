package com.bank.platform.proposal.domain;

/**
 * A proposal state rule refused a change. {@code code} is an {@code ErrorCodes} value; the
 * application layer turns it into the catalogue error.
 */
public class ProposalRuleException extends RuntimeException {

  private final String code;

  public ProposalRuleException(String code, String reason) {
    super(reason);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
