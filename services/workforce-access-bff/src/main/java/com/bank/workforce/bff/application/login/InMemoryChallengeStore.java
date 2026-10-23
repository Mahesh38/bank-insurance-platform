package com.bank.workforce.bff.application.login;

import com.bank.workforce.bff.session.SessionModels.ClientType;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Single-JVM challenge state for the v3.1 login journey. Captcha text and OTP values are stored
 * only as hashes. A multi-pod deployment needs a shared store before this journey is used there.
 */
@Component
public class InMemoryChallengeStore {

  private final ConcurrentHashMap<String, Captcha> captchas = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, OtpChallenge> otps = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Continuation> continuations = new ConcurrentHashMap<>();

  public synchronized void putCaptcha(String id, String hash, Instant expiresAt) {
    evict(Instant.now());
    captchas.put(id, new Captcha(hash, expiresAt));
  }

  public synchronized Optional<Captcha> takeCaptcha(String id) {
    return Optional.ofNullable(captchas.remove(id));
  }

  public synchronized OtpVerdict submitOtp(String id, String code, Instant now, int maxFailures) {
    OtpChallenge challenge = otps.get(id);
    if (challenge == null) {
      return OtpVerdict.missing();
    }
    if (now.isAfter(challenge.expiresAt())) {
      return new OtpVerdict(OtpCheck.EXPIRED, challenge);
    }
    if (!OtpDigest.matches(code, challenge.salt(), challenge.hash())) {
      int failures = challenge.failures() + 1;
      if (failures >= maxFailures) {
        otps.remove(id);
        return new OtpVerdict(OtpCheck.LOCKED_OUT, challenge);
      }
      OtpChallenge updated = challenge.withFailures(failures);
      otps.put(id, updated);
      return new OtpVerdict(OtpCheck.REJECTED, updated);
    }
    otps.remove(id);
    return new OtpVerdict(OtpCheck.ACCEPTED, challenge);
  }

  public synchronized ResendVerdict resend(
      String id,
      java.util.function.Supplier<String> code,
      Instant now,
      Instant expiresAt,
      Instant resendNotBefore,
      int maxResends) {
    OtpChallenge challenge = otps.get(id);
    if (challenge == null) {
      return ResendVerdict.missing();
    }
    if (challenge.resendsUsed() >= maxResends) {
      otps.remove(id);
      return new ResendVerdict(ResendCheck.LIMIT, null);
    }
    if (now.isBefore(challenge.resendNotBefore())) {
      return new ResendVerdict(ResendCheck.TOO_SOON, challenge);
    }
    String salt = OtpDigest.newSalt();
    String next = code.get();
    OtpChallenge updated =
        challenge.withNewCode(
            salt,
            OtpDigest.hash(salt, next),
            expiresAt,
            resendNotBefore,
            challenge.resendsUsed() + 1);
    otps.put(id, updated);
    return new ResendVerdict(ResendCheck.SENT, updated);
  }

  public synchronized void putOtp(OtpChallenge challenge) {
    evict(Instant.now());
    otps.put(challenge.id(), challenge);
  }

  public synchronized void putContinuation(Continuation continuation) {
    evict(Instant.now());
    continuations.put(continuation.id(), continuation);
  }

  public synchronized Optional<Continuation> takeContinuation(String id) {
    return Optional.ofNullable(continuations.remove(id));
  }

  private void evict(Instant now) {
    dropExpired(captchas.values().iterator(), now);
    dropOtp(now);
    Iterator<Continuation> continuationsLeft = continuations.values().iterator();
    while (continuationsLeft.hasNext()) {
      if (now.isAfter(continuationsLeft.next().expiresAt())) {
        continuationsLeft.remove();
      }
    }
  }

  private void dropOtp(Instant now) {
    Iterator<OtpChallenge> left = otps.values().iterator();
    while (left.hasNext()) {
      if (now.isAfter(left.next().expiresAt().plusSeconds(60))) {
        left.remove();
      }
    }
  }

  private static void dropExpired(Iterator<Captcha> left, Instant now) {
    while (left.hasNext()) {
      if (now.isAfter(left.next().expiresAt())) {
        left.remove();
      }
    }
  }

  public enum OtpCheck {
    MISSING,
    EXPIRED,
    REJECTED,
    LOCKED_OUT,
    ACCEPTED
  }

  public enum ResendCheck {
    MISSING,
    TOO_SOON,
    LIMIT,
    SENT
  }

  public record Captcha(String hash, Instant expiresAt) {}

  public record HeldAccount(
      UUID businessUserId,
      String userType,
      String status,
      long policyVersion,
      String insurerCode,
      String firstName,
      String username,
      String maskedMobile,
      String maskedEmail) {}

  public record OtpChallenge(
      String id,
      String purpose,
      ClientType clientType,
      HeldAccount account,
      String salt,
      String hash,
      Instant expiresAt,
      Instant resendNotBefore,
      int failures,
      int resendsUsed) {

    OtpChallenge withFailures(int failures) {
      return new OtpChallenge(
          id,
          purpose,
          clientType,
          account,
          salt,
          hash,
          expiresAt,
          resendNotBefore,
          failures,
          resendsUsed);
    }

    OtpChallenge withNewCode(
        String salt, String hash, Instant expiresAt, Instant resendNotBefore, int resendsUsed) {
      return new OtpChallenge(
          id,
          purpose,
          clientType,
          account,
          salt,
          hash,
          expiresAt,
          resendNotBefore,
          failures,
          resendsUsed);
    }
  }

  public record Continuation(
      String id, String purpose, ClientType clientType, HeldAccount account, Instant expiresAt) {}

  public record OtpVerdict(OtpCheck check, OtpChallenge challenge) {
    static OtpVerdict missing() {
      return new OtpVerdict(OtpCheck.MISSING, null);
    }
  }

  public record ResendVerdict(ResendCheck check, OtpChallenge challenge) {
    static ResendVerdict missing() {
      return new ResendVerdict(ResendCheck.MISSING, null);
    }
  }
}
