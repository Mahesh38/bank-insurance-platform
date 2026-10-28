package com.bank.platform.quotation.adapter.consent;

import com.bank.platform.quotation.domain.ConsentCheckpointPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local/test stub — always satisfied. Returns a fixed consent id. Property: {@code
 * quotation.gates.stub=true}.
 */
@Component
@ConditionalOnProperty(name = "quotation.gates.stub", havingValue = "true")
public class StubConsentCheckpointAdapter implements ConsentCheckpointPort {

  /** Stable ULID so working-store fixtures stay deterministic across stub runs. */
  public static final String STUB_CONSENT_ID = "01JZSTBBC0NSENT00000000001";

  @Override
  public String requireBeforeMultiQuote(String customerId) {
    return STUB_CONSENT_ID;
  }
}
