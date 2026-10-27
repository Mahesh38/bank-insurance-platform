package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ConsentLink;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.LinkPorts;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.QuoteShareStatement;
import com.bank.platform.consent.domain.Ulids;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suitability BRD 9.10. After the proposer questions, a secure link is sent to the registered
 * mobile and the registered email. The customer opens it, reads the wording, and agrees or
 * disagrees. The raw token is not returned to the seller.
 */
@Service
public class ConsentLinkService {

  private final LinkPorts.Links links;
  private final LinkPorts.Tokens tokens;
  private final LinkPorts.Dispatch dispatch;
  private final LinkPorts.Origin origin;
  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.ConsentStore consents;
  private final ConsentEvidence evidence;
  private final FieldCipher cipher;
  private final Clock clock;
  private final int ttlHours;
  private final int maxIssues;

  public ConsentLinkService(
      LinkPorts.Links links,
      LinkPorts.Tokens tokens,
      LinkPorts.Dispatch dispatch,
      LinkPorts.Origin origin,
      ConsentPorts.PolicySource policies,
      ConsentPorts.ConsentStore consents,
      ConsentEvidence evidence,
      FieldCipher cipher,
      Clock clock,
      @Value("${consent.link-ttl-hours:24}") int ttlHours,
      @Value("${consent.link-max-issues-per-24h:5}") int maxIssues) {
    this.links = links;
    this.tokens = tokens;
    this.dispatch = dispatch;
    this.origin = origin;
    this.policies = policies;
    this.consents = consents;
    this.evidence = evidence;
    this.cipher = cipher;
    this.clock = clock;
    this.ttlHours = ttlHours;
    this.maxIssues = maxIssues;
  }

  public boolean holdsQuoteShare(String customerId) {
    requireUlid("customerId", customerId);
    Instant now = clock.instant();
    return consents
        .head(customerId, QuoteShareStatement.PURPOSE, null)
        .filter(row -> "GRANTED".equals(row.state()))
        .filter(row -> row.validUntil() == null || now.isBefore(row.validUntil()))
        .isPresent();
  }

  @Transactional
  public LinkView issue(IssueCommand command, String actorId) {
    return send(command, actorId, false);
  }

  @Transactional
  public LinkView resend(String assessmentId, String actorId) {
    requireUlid("assessmentId", assessmentId);
    ConsentLink existing =
        links
            .pending(assessmentId)
            .or(() -> links.latest(assessmentId))
            .orElseThrow(
                () -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent link was not found"));
    IssueCommand command =
        new IssueCommand(
            existing.customerId(),
            existing.leadId(),
            existing.journeyId(),
            assessmentId,
            cipher.decrypt(existing.mobileEnc()),
            cipher.decrypt(existing.emailEnc()));
    return send(command, actorId, true);
  }

  public LinkView status(String assessmentId) {
    requireUlid("assessmentId", assessmentId);
    ConsentLink link =
        links
            .pending(assessmentId)
            .or(() -> links.latest(assessmentId))
            .orElseThrow(
                () -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent link was not found"));
    return view(effective(link, clock.instant()), false);
  }

  /** Customer opened the link. No customer identifier is read from the request. */
  @Transactional
  public PublicPage open(String token) {
    return present(token, null, null);
  }

  @Transactional
  public PublicPage decide(String token, String decision, String sourceIp) {
    if (sourceIp == null || sourceIp.isBlank()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "source address is required");
    }
    return present(token, decision, trimIp(sourceIp));
  }

  private LinkView send(IssueCommand command, String actorId, boolean force) {
    String customerId = requireUlid("customerId", command.customerId());
    String leadId = requireUlid("leadId", command.leadId());
    String assessmentId = requireUlid("assessmentId", command.assessmentId());
    if (command.journeyId() != null && !command.journeyId().isBlank()) {
      requireUlid("journeyId", command.journeyId());
    }
    if (actorId == null || actorId.isBlank()) {
      throw new ConsentRefusal("UNAUTHORIZED", "actor is required");
    }
    String mobile = mobile(command.mobile());
    String email = email(command.email());
    Instant now = clock.instant();
    Optional<ConsentLink> current = links.pending(assessmentId);
    if (current.isPresent()) {
      ConsentLink link = effective(current.get(), now);
      if ("PENDING".equals(link.state()) && !force) {
        return view(link, false);
      }
    }
    if (links.countIssuedSince(customerId, now.minus(Duration.ofHours(24))) >= maxIssues) {
      throw new ConsentRefusal("OTP_ISSUE_RATE_EXCEEDED", "link issue limit reached");
    }
    if (current.isPresent() && "PENDING".equals(current.get().state())) {
      links.update(current.get().supersede());
    }
    PolicySnapshot policy = policies.current();
    String token = tokens.mint();
    ConsentLink created =
        new ConsentLink(
            Ulids.mint(clock),
            tokens.hash(token),
            customerId,
            leadId,
            blank(command.journeyId()) ? null : command.journeyId(),
            assessmentId,
            cipher.keyId(),
            cipher.encrypt(mobile),
            cipher.encrypt(email),
            maskMobile(mobile),
            maskEmail(email),
            "PENDING",
            now.plus(Duration.ofHours(ttlHours)),
            now,
            null,
            null,
            null,
            actorId,
            policy.versionRef());
    links.insert(created);
    links.recordIssue(Ulids.mint(clock), customerId, assessmentId, now);
    dispatch.send(mobile, email, origin.url(token));
    return view(created, true);
  }

  private PublicPage present(String token, String decision, String sourceIp) {
    if (!tokens.wellFormed(token)) {
      return PublicPage.invalid();
    }
    Instant now = clock.instant();
    Optional<ConsentLink> found =
        decision == null
            ? links.findByHash(tokens.hash(token))
            : links.lockByHash(tokens.hash(token));
    if (found.isEmpty()) {
      return PublicPage.invalid();
    }
    ConsentLink link = found.get();
    if ("PENDING".equals(link.state()) && !now.isBefore(link.expiresAt())) {
      links.update(link.decide("EXPIRED", now, link.sourceIp(), link.consentId()));
      return PublicPage.expired();
    }
    if (!"PENDING".equals(link.state())) {
      return PublicPage.used(link.state());
    }
    if (decision == null) {
      return PublicPage.open();
    }
    if (!"AGREE".equals(decision) && !"DISAGREE".equals(decision)) {
      throw new ConsentRefusal("VALIDATION_ERROR", "decision is not valid");
    }
    if ("DISAGREE".equals(decision)) {
      links.update(link.decide("DISAGREED", now, sourceIp, null));
      return PublicPage.disagreed();
    }
    PolicySnapshot policy = policies.current();
    String consentId =
        evidence.grantQuoteShare(
            link.customerId(),
            link.leadId(),
            link.journeyId(),
            link.mobileEnc(),
            link.mobileEncKeyId(),
            sourceIp,
            now,
            policy);
    links.update(link.decide("AGREED", now, sourceIp, consentId));
    return PublicPage.agreed();
  }

  private static ConsentLink effective(ConsentLink link, Instant now) {
    if ("PENDING".equals(link.state()) && !now.isBefore(link.expiresAt())) {
      return link.decide("EXPIRED", now, link.sourceIp(), link.consentId());
    }
    return link;
  }

  private static LinkView view(ConsentLink link, boolean sent) {
    return new LinkView(
        link.assessmentId(),
        link.state(),
        link.mobileMasked(),
        link.emailMasked(),
        link.expiresAt(),
        sent);
  }

  private static String requireUlid(String field, String value) {
    if (!Ulids.isValid(value)) {
      throw new ConsentRefusal("VALIDATION_ERROR", field + " must be a ULID");
    }
    return value;
  }

  private static String trimIp(String sourceIp) {
    String ip = sourceIp.trim();
    return ip.length() > 64 ? ip.substring(0, 64) : ip;
  }

  static String mobile(String raw) {
    if (raw == null) {
      throw new ConsentRefusal("VALIDATION_ERROR", "registered mobile is required");
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.length() == 12 && digits.startsWith("91")) {
      digits = digits.substring(2);
    }
    if (digits.length() == 11 && digits.startsWith("0")) {
      digits = digits.substring(1);
    }
    if (!digits.matches("^[6-9][0-9]{9}$")) {
      throw new ConsentRefusal("VALIDATION_ERROR", "registered mobile is not valid");
    }
    return digits;
  }

  static String email(String raw) {
    if (raw == null) {
      throw new ConsentRefusal("VALIDATION_ERROR", "registered email is required");
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.length() > 120 || !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      throw new ConsentRefusal("VALIDATION_ERROR", "registered email is not valid");
    }
    return value;
  }

  static String maskMobile(String digits) {
    return "+91 " + digits.substring(0, 2) + "*****" + digits.substring(7);
  }

  static String maskEmail(String value) {
    int at = value.indexOf('@');
    return value.substring(0, 1) + "***" + value.substring(at);
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public record IssueCommand(
      String customerId,
      String leadId,
      String journeyId,
      String assessmentId,
      String mobile,
      String email) {}

  public record LinkView(
      String assessmentId,
      String state,
      String maskedMobile,
      String maskedEmail,
      Instant expiresAt,
      boolean sent) {}

  public record PublicPage(String kind, String statement) {
    static PublicPage open() {
      return new PublicPage("OPEN", QuoteShareStatement.TEXT);
    }

    static PublicPage agreed() {
      return new PublicPage("AGREED", QuoteShareStatement.TEXT);
    }

    static PublicPage disagreed() {
      return new PublicPage("DISAGREED", QuoteShareStatement.TEXT);
    }

    static PublicPage expired() {
      return new PublicPage("EXPIRED", null);
    }

    static PublicPage invalid() {
      return new PublicPage("INVALID", null);
    }

    static PublicPage used(String state) {
      if ("AGREED".equals(state)) {
        return agreed();
      }
      if ("DISAGREED".equals(state)) {
        return disagreed();
      }
      if ("EXPIRED".equals(state)) {
        return expired();
      }
      return new PublicPage("USED", null);
    }
  }
}
