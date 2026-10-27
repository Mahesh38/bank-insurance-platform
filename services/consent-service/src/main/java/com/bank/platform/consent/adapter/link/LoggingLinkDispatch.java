package com.bank.platform.consent.adapter.link;

import com.bank.platform.consent.domain.LinkPorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hands the link to the SMS and email gateway. The address and the URL are not logged. The bank
 * gateway is not connected in this build; the call still fails closed if the payload is incomplete.
 */
public final class LoggingLinkDispatch implements LinkPorts.Dispatch {

  private static final Logger log = LoggerFactory.getLogger(LoggingLinkDispatch.class);

  @Override
  public void send(String mobileDigits, String emailAddress, String url) {
    if (mobileDigits == null || mobileDigits.isBlank()) {
      throw new IllegalArgumentException("mobile");
    }
    if (emailAddress == null || emailAddress.isBlank()) {
      throw new IllegalArgumentException("email");
    }
    if (url == null || !url.contains("/public/v1/consent-links/")) {
      throw new IllegalArgumentException("url");
    }
    log.info("consent link dispatch accepted");
  }
}
