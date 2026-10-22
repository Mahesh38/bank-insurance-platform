package com.bank.platform.consent.domain;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The set of consent types the process can adapt to. Lookup is by code, not by a switch in callers.
 */
public final class ConsentPolicyRegistry {

  private final Map<ConsentCode, ConsentTypePolicy> policies = new EnumMap<>(ConsentCode.class);

  public ConsentPolicyRegistry(List<ConsentTypePolicy> installed) {
    for (ConsentTypePolicy policy : installed) {
      ConsentTypePolicy previous = policies.put(policy.code(), policy);
      if (previous != null) {
        throw new IllegalStateException("duplicate policy for " + policy.code().wire());
      }
    }
    for (ConsentCode code : ConsentCode.values()) {
      if (!policies.containsKey(code)) {
        throw new IllegalStateException("missing policy for " + code.wire());
      }
    }
  }

  public static ConsentPolicyRegistry r0() {
    return new ConsentPolicyRegistry(
        List.of(
            new DataProcessingPolicy(),
            new SolicitationPolicy(),
            new SharingPolicy(),
            new CommunicationsPolicy()));
  }

  public ConsentTypePolicy require(ConsentCode code) {
    ConsentTypePolicy policy = policies.get(code);
    if (policy == null) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "no policy for consent code");
    }
    return policy;
  }
}
