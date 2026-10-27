package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.Optional;

/** Ports for the public consent link. Adapters live outside the domain. */
public final class LinkPorts {

  private LinkPorts() {}

  public interface Tokens {
    String mint();

    String hash(String token);

    boolean wellFormed(String token);
  }

  public interface Dispatch {
    void send(String mobileDigits, String emailAddress, String url);
  }

  public interface Origin {
    String url(String token);
  }

  public interface Links {
    void insert(ConsentLink link);

    void update(ConsentLink link);

    Optional<ConsentLink> findByHash(String tokenHash);

    Optional<ConsentLink> lockByHash(String tokenHash);

    Optional<ConsentLink> pending(String assessmentId);

    Optional<ConsentLink> latest(String assessmentId);

    int countIssuedSince(String customerId, Instant since);

    void recordIssue(String issueId, String customerId, String assessmentId, Instant at);
  }
}
