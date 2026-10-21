package com.bank.platform.lead.domain;

/**
 * The PDP, asked again by Lead itself before a mutating command (INV-ACT-01): a BFF allow is not
 * trusted across the hop. Implementations fail closed — no answer is {@link
 * AuthorizationDecision#unavailable()}, never an allow.
 */
public interface LeadAuthorizationPort {

  AuthorizationDecision decide(String principalId, String action, String leadId, String branchId);

  record AuthorizationDecision(boolean allowed, String reasonCode, boolean unavailable) {

    public static AuthorizationDecision allow(String reasonCode) {
      return new AuthorizationDecision(true, reasonCode, false);
    }

    public static AuthorizationDecision deny(String reasonCode) {
      return new AuthorizationDecision(false, reasonCode, false);
    }

    public static AuthorizationDecision unavailableDecision() {
      return new AuthorizationDecision(false, "PDP_UNAVAILABLE", true);
    }
  }
}
