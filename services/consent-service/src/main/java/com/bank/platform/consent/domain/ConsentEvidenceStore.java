package com.bank.platform.consent.domain;

import java.util.List;
import java.util.Optional;

/** Insert-only evidence store. There is no update and no delete. */
public interface ConsentEvidenceStore {

  void append(ConsentEvidence evidence);

  List<ConsentEvidence> chain(String rootConsentId);

  List<ConsentEvidence> byJourney(String journeyId);

  List<ConsentEvidence> byCustomer(String customerId);

  Optional<ConsentEvidence> head(String customerId, ConsentCode code, String counterpartyId);
}
