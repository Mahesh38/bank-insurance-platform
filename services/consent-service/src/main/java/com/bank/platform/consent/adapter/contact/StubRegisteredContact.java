package com.bank.platform.consent.adapter.contact;

import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import org.springframework.stereotype.Component;

/**
 * Stand-in for customer-service {@code getRegisteredContact}. The number is never from the body.
 */
@Component
public class StubRegisteredContact implements ConsentPorts.RegisteredContact {

  private volatile boolean unavailable;

  public void setUnavailable(boolean unavailable) {
    this.unavailable = unavailable;
  }

  @Override
  public String digits(String customerId) {
    if (unavailable) {
      throw new ConsentRefusal("UPSTREAM_UNAVAILABLE", "registered contact is unavailable");
    }
    return "919876543210";
  }
}
