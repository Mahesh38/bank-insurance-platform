package com.bank.platform.quotation.adapter.suitability;

import com.bank.platform.quotation.domain.SuitabilityGatePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Local/test stub — always allows. Property: {@code quotation.gates.stub=true}. */
@Component
@ConditionalOnProperty(name = "quotation.gates.stub", havingValue = "true")
public class StubSuitabilityGateAdapter implements SuitabilityGatePort {

  @Override
  public void requireUsable(
      String customerId, String lob, String productClass, String evaluationId) {
    // always usable
  }
}
