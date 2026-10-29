package com.bank.identity.provider.domain;

/**
 * Insurance-user credential check against Keycloak. Insurance RM, FLS, Insurance Partner and
 * Partner representative are this one population. The BFF must never call Keycloak itself.
 */
public interface InsuranceCredentialVerifier {

  Result verify(Command command);

  void replacePassword(ReplaceCommand command);

  record Command(String username, String password) {
    @Override
    public String toString() {
      return "Command[username-present=" + (username != null && !username.isBlank()) + "]";
    }
  }

  record ReplaceCommand(String username, String password) {
    @Override
    public String toString() {
      return "ReplaceCommand[username-present=" + (username != null && !username.isBlank()) + "]";
    }
  }

  record Result(boolean authenticated, boolean accountActive) {}
}
