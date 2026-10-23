package com.bank.platform.consent.application;

import com.bank.platform.consent.application.ConsentTypes.ChainView;
import com.bank.platform.consent.application.ConsentTypes.ConsentView;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.SecurityEventSink;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Reads one consent chain, or every chain for one customer or one journey. */
@Service
public class ReadConsent {

  private final ConsentPorts.ConsentStore consents;
  private final ConsentEvidence evidence;
  private final SecurityEventSink security;

  public ReadConsent(
      ConsentPorts.ConsentStore consents, ConsentEvidence evidence, SecurityEventSink security) {
    this.consents = consents;
    this.evidence = evidence;
    this.security = security;
  }

  public ConsentView get(String consentId, String customerId, String actorId, String journeyId) {
    ConsentInput.requireUlid("consentId", consentId);
    if (customerId != null) {
      ConsentInput.requireUlid("customerId", customerId);
    }
    ConsentRow head =
        consents
            .headOf(consentId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent was not found"));
    if (customerId != null && !customerId.equals(head.customerId())) {
      security.subjectMismatch(
          actorId, journeyId == null ? "-" : journeyId, consentId, customerId, head.customerId());
      throw new ConsentRefusal("CONSENT_SUBJECT_MISMATCH", "consent subject does not match");
    }
    return evidence.consentView(head);
  }

  public List<ChainView> list(String customerId, String journeyId) {
    boolean byCustomer = customerId != null && !customerId.isBlank();
    boolean byJourney = journeyId != null && !journeyId.isBlank();
    if (byCustomer == byJourney) {
      throw new ConsentRefusal(
          "VALIDATION_ERROR", "exactly one of customer or journey is required");
    }
    List<String> ids =
        byCustomer
            ? consents.chainIdsForCustomer(customerId)
            : consents.chainIdsForJourney(journeyId);
    if (byCustomer) {
      ConsentInput.requireUlid("customerId", customerId);
    } else {
      ConsentInput.requireUlid("journeyId", journeyId);
    }
    List<ChainView> chains = new ArrayList<>();
    for (String id : ids) {
      chains.add(
          new ChainView(id, consents.chain(id).stream().map(evidence::consentView).toList()));
    }
    return chains;
  }
}
