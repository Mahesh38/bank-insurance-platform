package com.bank.platform.suitability.support;

import com.bank.platform.suitability.domain.JourneyModel.ConsentLinks;
import com.bank.platform.suitability.domain.JourneyModel.LinkReceipt;
import com.bank.platform.suitability.domain.JourneyModel.LinkRequest;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Test double for the consent link. Production uses the HTTP client. */
@Component
@ConditionalOnProperty(name = "suitability.journey.consent-client", havingValue = "fake")
public class FakeConsentLinks implements ConsentLinks {

  public boolean granted;
  public int issues;
  public String state = "ABSENT";

  public void reset() {
    granted = false;
    issues = 0;
    state = "ABSENT";
  }

  @Override
  public boolean holdsQuoteShare(String customerId) {
    return granted;
  }

  @Override
  public LinkReceipt issue(LinkRequest request) {
    if ("PENDING".equals(state)) {
      return receipt(false);
    }
    issues++;
    state = "PENDING";
    return receipt(true);
  }

  @Override
  public LinkReceipt resend(String assessmentId, String actorId) {
    if ("ABSENT".equals(state)) {
      throw new SuitabilityRefusal("RESOURCE_NOT_FOUND", "consent link was not found");
    }
    issues++;
    state = "PENDING";
    return receipt(true);
  }

  @Override
  public String linkState(String assessmentId) {
    return state;
  }

  private LinkReceipt receipt(boolean sent) {
    return new LinkReceipt(
        state, "+91 98*****210", "a***@example.com", Instant.parse("2026-10-28T06:30:00Z"), sent);
  }
}
