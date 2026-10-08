package com.bank.identity.provider.domain;

/**
 * Bank-employee credential check against the existing AD-verify API. The BFF must never call AD
 * itself; this port is the only outbound hop.
 */
public interface WorkforceCredentialVerifier {

  AdVerifyResult verify(AdVerifyCommand command);

  record AdVerifyCommand(String employeeId, String password) {
    @Override
    public String toString() {
      return "AdVerifyCommand[employeeId-present="
          + (employeeId != null && !employeeId.isBlank())
          + "]";
    }
  }

  record AdVerifyResult(
      boolean authenticated,
      boolean accountActive,
      String employeeId,
      String username,
      String email) {
    public boolean accepted() {
      return authenticated && accountActive;
    }
  }
}
