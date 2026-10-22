package com.bank.platform.consent.domain;

public enum ConsentChannel {
  ASSISTED,
  DIY,
  HYBRID;

  public static ConsentChannel parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "channel is required");
    }
    for (ConsentChannel channel : values()) {
      if (channel.name().equals(raw.trim())) {
        return channel;
      }
    }
    throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "unknown channel");
  }
}
