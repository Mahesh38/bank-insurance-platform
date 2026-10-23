package com.bank.platform.quotation.domain;

/** Offer state inside the envelope; same values as CHECK {@code ck_offer_state}. */
public enum OfferState {
  QUOTED,
  SELECTED,
  NOT_SELECTED,
  INVALID
}
