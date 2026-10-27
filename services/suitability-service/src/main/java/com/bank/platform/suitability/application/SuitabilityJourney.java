package com.bank.platform.suitability.application;

import com.bank.platform.suitability.application.AssessmentView.ConsentPanel;
import com.bank.platform.suitability.application.AssessmentView.DerivedView;
import com.bank.platform.suitability.application.AssessmentView.ExitCopy;
import com.bank.platform.suitability.application.AssessmentView.ProductView;
import com.bank.platform.suitability.domain.Assessment;
import com.bank.platform.suitability.domain.BrdCopy;
import com.bank.platform.suitability.domain.Contacts;
import com.bank.platform.suitability.domain.JourneyModel.Assessments;
import com.bank.platform.suitability.domain.JourneyModel.ConsentLinks;
import com.bank.platform.suitability.domain.JourneyModel.FieldCipher;
import com.bank.platform.suitability.domain.JourneyModel.FieldError;
import com.bank.platform.suitability.domain.JourneyModel.History;
import com.bank.platform.suitability.domain.JourneyModel.IncomeBand;
import com.bank.platform.suitability.domain.JourneyModel.LinkReceipt;
import com.bank.platform.suitability.domain.JourneyModel.LinkRequest;
import com.bank.platform.suitability.domain.JourneyModel.Masters;
import com.bank.platform.suitability.domain.JourneyModel.ProductGrid;
import com.bank.platform.suitability.domain.JourneyModel.ProductMatch;
import com.bank.platform.suitability.domain.JourneyModel.RawAnswers;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import com.bank.platform.suitability.domain.SuitabilityRules;
import com.bank.platform.suitability.domain.Ulids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suitability BRD current design. Explore matching plans sends the section 9.10 link unless this
 * customer already holds quote-share consent. Suitable products are opened only after that consent.
 * Premium, premium paying term and policy term stay on the interim section 22 rule.
 */
@Service
public class SuitabilityJourney {

  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
  private static final String NOT_STARTED = "NOT_STARTED";
  private static final String IN_PROGRESS = "IN_PROGRESS";
  private static final String COMPLETED = "COMPLETED";
  private static final String NONE = "NO_SUITABLE_PRODUCT_FOUND";
  private static final String LOCKED = "COMPLETED_AND_LOCKED";

  private final Assessments assessments;
  private final History history;
  private final Masters masters;
  private final ProductGrid grid;
  private final FieldCipher cipher;
  private final ConsentLinks links;
  private final ObjectMapper mapper;
  private final Clock clock;
  private final int lockMinutes;
  private final String ruleId;
  private final String frequency;
  private final String payingTerm;
  private final String policyTerm;
  private final int premiumPercent;

  public SuitabilityJourney(
      Assessments assessments,
      History history,
      Masters masters,
      ProductGrid grid,
      FieldCipher cipher,
      ConsentLinks links,
      ObjectMapper mapper,
      Clock clock,
      @Value("${suitability.journey.lock-minutes:15}") int lockMinutes,
      @Value("${suitability.journey.derivation-rule-id}") String ruleId,
      @Value("${suitability.journey.derivation-frequency}") String frequency,
      @Value("${suitability.journey.derivation-premium-paying-term}") String payingTerm,
      @Value("${suitability.journey.derivation-policy-term}") String policyTerm,
      @Value("${suitability.journey.derivation-premium-percent:10}") int premiumPercent) {
    this.assessments = assessments;
    this.history = history;
    this.masters = masters;
    this.grid = grid;
    this.cipher = cipher;
    this.links = links;
    this.mapper = mapper;
    this.clock = clock;
    this.lockMinutes = lockMinutes;
    this.ruleId = ruleId;
    this.frequency = frequency;
    this.payingTerm = payingTerm;
    this.policyTerm = policyTerm;
    this.premiumPercent = premiumPercent;
  }

  @Transactional
  public AssessmentView create(CreateCommand command, String actorId) {
    String actor = actor(actorId);
    String product = SuitabilityRules.productType(command.productType());
    if (product == null) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", null, "productType", BrdCopy.PRODUCT_TYPE);
    }
    String leadId = ulid("leadId", command.leadId());
    String journeyId = ulid("journeyId", command.journeyId());
    String customerId = ulid("customerId", command.customerId());
    if (assessments.leadExists(leadId)) {
      throw new SuitabilityRefusal("CONFLICT", "an assessment already exists for this lead");
    }
    if (!actor.equals(command.spActorId()) && !actor.equals(command.rmActorId())) {
      throw new SuitabilityRefusal("FORBIDDEN", "actor is not an owner of this assessment");
    }
    String name = BrdCopy.normaliseName(command.proposerName());
    if (name.isBlank()) {
      throw new SuitabilityRefusal(
          "VALIDATION_ERROR", null, "proposerName", "Proposer name is required.");
    }
    if (!BrdCopy.personName(name)) {
      throw SuitabilityRefusal.validation(
          new FieldError("VAL-003", "proposerName", BrdCopy.VAL_003));
    }
    LocalDate dob = proposerDob(command.proposerDob());
    String mobile = Contacts.requireMobile(command.registeredMobile());
    String email = Contacts.requireEmail(command.registeredEmail());
    requireActor(command.spActorId(), "spActorId");
    requireActor(command.rmActorId(), "rmActorId");
    Instant now = clock.instant();
    Assessment assessment = new Assessment();
    assessment.setAssessmentId(Ulids.mint(clock));
    assessment.setLeadId(leadId);
    assessment.setJourneyId(journeyId);
    assessment.setCustomerId(customerId);
    assessment.setProductType(product);
    assessment.setProposerName(name);
    assessment.setProposerDob(dob);
    assessment.setStatus(NOT_STARTED);
    assessment.setLastScreen("POLICY_FOR");
    assessment.setEncryptionKeyId(cipher.keyId());
    assessment.setMobileEnc(cipher.encrypt(mobile));
    assessment.setEmailEnc(cipher.encrypt(email));
    assessment.setMobileMasked(Contacts.maskMobile(mobile));
    assessment.setEmailMasked(Contacts.maskEmail(email));
    assessment.setSpActorId(command.spActorId().trim());
    assessment.setRmActorId(command.rmActorId().trim());
    assessment.setConsentState("ABSENT");
    assessment.setAwaitingConsent(false);
    assessment.setMappingRunNo(0);
    assessment.setShowProducts(false);
    assessment.setResponseVersion(1);
    assessment.setAnswers("{}");
    assessment.setCreatedBy(actor);
    assessment.setCreatedAt(now);
    assessment.setUpdatedBy(actor);
    assessment.setUpdatedAt(now);
    assessments.insert(assessment);
    return view(assessment);
  }

  @Transactional
  public AssessmentView get(String id, String actorId) {
    Assessment assessment = owned(id, actor(actorId));
    if (assessment.isAwaitingConsent()) {
      String state = links.linkState(assessment.getAssessmentId());
      assessment.setConsentState(state);
      if ("AGREED".equals(state) || links.holdsQuoteShare(assessment.getCustomerId())) {
        map(assessment, actor(actorId), clock.instant());
      }
      touch(assessment, actor(actorId), clock.instant());
      assessments.update(assessment);
    }
    return view(assessment);
  }

  @Transactional
  public AssessmentView saveAnswers(String id, JsonNode patch, String actorId) {
    String actor = actor(actorId);
    Assessment assessment = owned(id, actor);
    rejectIfImmutable(assessment);
    holdLock(assessment, actor, clock.instant());
    String merged = AnswerSheet.merge(assessment.getAnswers(), patch, mapper);
    boolean changed = !AnswerSheet.same(assessment.getAnswers(), merged, mapper);
    if (changed) {
      boolean hadProducts =
          assessment.isShowProducts()
              || COMPLETED.equals(assessment.getStatus())
              || NONE.equals(assessment.getStatus());
      assessment.setAnswers(merged);
      assessment.setShowProducts(false);
      assessment.setProducts(List.of());
      assessment.setSelectedProductId(null);
      assessment.setAwaitingConsent(false);
      assessment.setStatus(IN_PROGRESS);
      assessment.setNotice(hadProducts ? BrdCopy.VAL_032 : null);
    }
    RawAnswers answers = AnswerSheet.read(assessment.getAnswers(), mapper);
    SuitabilityRules.save(answers, masters, today())
        .ifPresent(
            error -> {
              throw SuitabilityRefusal.validation(error);
            });
    assessment.setLastScreen(AnswerSheet.screen(patch, answers));
    Instant now = clock.instant();
    touch(assessment, actor, now);
    assessments.update(assessment);
    history.answer(
        assessment.getAssessmentId(),
        assessment.getLastScreen(),
        assessment.getAnswers(),
        actor,
        now);
    return view(assessment);
  }

  @Transactional
  public AssessmentView saveExit(String id, String actorId) {
    String actor = actor(actorId);
    Assessment assessment = owned(id, actor);
    rejectIfImmutable(assessment);
    Instant now = clock.instant();
    if (heldByOther(assessment, actor, now)) {
      throw new SuitabilityRefusal("CONFLICT", "VAL-031", null, BrdCopy.VAL_031);
    }
    assessment.setLockActorId(null);
    assessment.setLockUntil(null);
    touch(assessment, actor, now);
    assessments.update(assessment);
    return view(assessment);
  }

  @Transactional
  public AssessmentView explore(String id, String actorId) {
    String actor = actor(actorId);
    Assessment assessment = owned(id, actor);
    rejectIfImmutable(assessment);
    holdLock(assessment, actor, clock.instant());
    RawAnswers answers = AnswerSheet.read(assessment.getAnswers(), mapper);
    Optional<FieldError> gap = SuitabilityRules.explore(answers, masters, today());
    if (gap.isPresent()) {
      throw SuitabilityRefusal.validation(gap.get());
    }
    derive(assessment, answers);
    if (!assessment.isAwaitingConsent()
        && (COMPLETED.equals(assessment.getStatus()) || NONE.equals(assessment.getStatus()))) {
      return view(assessment);
    }
    boolean agreed = links.holdsQuoteShare(assessment.getCustomerId());
    if (!agreed && assessment.isAwaitingConsent()) {
      String state = links.linkState(assessment.getAssessmentId());
      assessment.setConsentState(state);
      agreed = "AGREED".equals(state);
    }
    Instant now = clock.instant();
    if (agreed) {
      map(assessment, actor, now);
    } else {
      LinkReceipt receipt =
          links.issue(
              new LinkRequest(
                  assessment.getCustomerId(),
                  assessment.getLeadId(),
                  assessment.getJourneyId(),
                  assessment.getAssessmentId(),
                  cipher.decrypt(assessment.getMobileEnc()),
                  cipher.decrypt(assessment.getEmailEnc()),
                  actor));
      assessment.setConsentState(receipt.state());
      assessment.setMobileMasked(receipt.maskedMobile());
      assessment.setEmailMasked(receipt.maskedEmail());
      assessment.setAwaitingConsent(true);
      assessment.setShowProducts(false);
      assessment.setProducts(List.of());
      assessment.setStatus(IN_PROGRESS);
      assessment.setLastScreen("CUSTOMER_CONSENT");
      assessment.setNotice(null);
    }
    touch(assessment, actor, now);
    assessments.update(assessment);
    history.answer(
        assessment.getAssessmentId(),
        assessment.getLastScreen(),
        assessment.getAnswers(),
        actor,
        now);
    return view(assessment);
  }

  @Transactional
  public AssessmentView resend(String id, String actorId) {
    String actor = actor(actorId);
    Assessment assessment = owned(id, actor);
    rejectIfImmutable(assessment);
    if (!assessment.isAwaitingConsent()) {
      throw new SuitabilityRefusal("ILLEGAL_TRANSITION", "consent link is not pending");
    }
    holdLock(assessment, actor, clock.instant());
    LinkReceipt receipt = links.resend(assessment.getAssessmentId(), actor);
    assessment.setConsentState(receipt.state());
    assessment.setMobileMasked(receipt.maskedMobile());
    assessment.setEmailMasked(receipt.maskedEmail());
    assessment.setAwaitingConsent(true);
    assessment.setLastScreen("CUSTOMER_CONSENT");
    assessment.setStatus(IN_PROGRESS);
    Instant now = clock.instant();
    touch(assessment, actor, now);
    assessments.update(assessment);
    return view(assessment);
  }

  @Transactional
  public AssessmentView finalBi(String id, String biReference, String actorId) {
    String actor = actor(actorId);
    Assessment assessment = owned(id, actor);
    rejectIfImmutable(assessment);
    if (!COMPLETED.equals(assessment.getStatus())) {
      throw new SuitabilityRefusal(
          "ILLEGAL_TRANSITION", "final Benefit Illustration requires a completed assessment");
    }
    if (biReference == null || biReference.isBlank() || biReference.trim().length() > 64) {
      throw new SuitabilityRefusal(
          "VALIDATION_ERROR", null, "biReference", "BI reference is required.");
    }
    assessment.setBiReference(biReference.trim());
    assessment.setStatus(LOCKED);
    if (assessment.getSelectedProductId() == null && assessment.getProducts().size() == 1) {
      assessment.setSelectedProductId(assessment.getProducts().get(0).productId());
    }
    assessment.setLockActorId(null);
    assessment.setLockUntil(null);
    Instant now = clock.instant();
    touch(assessment, actor, now);
    assessments.update(assessment);
    return view(assessment);
  }

  private void map(Assessment assessment, String actor, Instant now) {
    RawAnswers answers = AnswerSheet.read(assessment.getAnswers(), mapper);
    derive(assessment, answers);
    int age = SuitabilityRules.age(assessment.getProposerDob(), today());
    List<ProductMatch> matched = grid.match(assessment.getProductType(), age, answers);
    int run = assessment.getMappingRunNo() + 1;
    assessment.setMappingRunNo(run);
    assessment.setMappingVersion(grid.version());
    assessment.setConsentState("AGREED");
    assessment.setAwaitingConsent(false);
    assessment.setLastScreen("SUITABLE_PRODUCTS");
    assessment.setNotice(null);
    assessment.setLockActorId(null);
    assessment.setLockUntil(null);
    assessment.setProducts(matched);
    if (matched.isEmpty()) {
      assessment.setStatus(NONE);
      assessment.setShowProducts(false);
      assessment.setSelectedProductId(null);
    } else {
      assessment.setStatus(COMPLETED);
      assessment.setShowProducts(true);
      if (matched.size() == 1) {
        assessment.setSelectedProductId(matched.get(0).productId());
      }
    }
    history.mapping(
        assessment.getAssessmentId(),
        run,
        assessment.getMappingVersion(),
        assessment.getStatus(),
        actor,
        now,
        matched);
  }

  private void derive(Assessment assessment, RawAnswers answers) {
    IncomeBand band =
        masters
            .income(answers.incomeBand())
            .orElseThrow(
                () ->
                    SuitabilityRefusal.validation(
                        new FieldError("VAL-016", "incomeBand", BrdCopy.VAL_016)));
    long premium = band.floorInr() * premiumPercent / 100L;
    assessment.setPremiumInr(premium);
    assessment.setPremiumFrequency(frequency);
    assessment.setPremiumPayingTerm(payingTerm);
    assessment.setPolicyTerm(policyTerm);
    assessment.setDerivationRuleId(ruleId);
  }

  private Assessment owned(String id, String actor) {
    if (!Ulids.isValid(id)) {
      throw new SuitabilityRefusal(
          "VALIDATION_ERROR", null, "assessmentId", "assessmentId must be a ULID");
    }
    Assessment assessment =
        assessments
            .lock(id)
            .orElseThrow(
                () ->
                    new SuitabilityRefusal(
                        "RESOURCE_NOT_FOUND", "suitability assessment was not found"));
    if (!actor.equals(assessment.getSpActorId()) && !actor.equals(assessment.getRmActorId())) {
      throw new SuitabilityRefusal("FORBIDDEN", "actor is not an owner of this assessment");
    }
    return assessment;
  }

  private void rejectIfImmutable(Assessment assessment) {
    if (LOCKED.equals(assessment.getStatus())) {
      throw new SuitabilityRefusal("ASSESSMENT_IMMUTABLE", "VAL-030", null, BrdCopy.VAL_030);
    }
  }

  private void holdLock(Assessment assessment, String actor, Instant now) {
    if (heldByOther(assessment, actor, now)) {
      throw new SuitabilityRefusal("CONFLICT", "VAL-031", null, BrdCopy.VAL_031);
    }
    assessment.setLockActorId(actor);
    assessment.setLockUntil(now.plus(Duration.ofMinutes(lockMinutes)));
  }

  private static boolean heldByOther(Assessment assessment, String actor, Instant now) {
    return assessment.getLockActorId() != null
        && assessment.getLockUntil() != null
        && now.isBefore(assessment.getLockUntil())
        && !assessment.getLockActorId().equals(actor);
  }

  private void touch(Assessment assessment, String actor, Instant now) {
    assessment.setResponseVersion(assessment.getResponseVersion() + 1);
    assessment.setUpdatedBy(actor);
    assessment.setUpdatedAt(now);
  }

  private LocalDate today() {
    return LocalDate.now(clock.withZone(ZONE));
  }

  private LocalDate proposerDob(String raw) {
    if (Contacts.blank(raw)) {
      throw SuitabilityRefusal.validation(
          new FieldError("VAL-026", "proposerDob", BrdCopy.VAL_026));
    }
    return SuitabilityRules.pastDate(raw, today())
        .orElseThrow(
            () ->
                SuitabilityRefusal.validation(
                    new FieldError("VAL-026", "proposerDob", BrdCopy.VAL_026)));
  }

  private AssessmentView view(Assessment assessment) {
    boolean pending =
        assessment.isAwaitingConsent() && !"AGREED".equals(assessment.getConsentState());
    boolean received =
        "AGREED".equals(assessment.getConsentState())
            && (COMPLETED.equals(assessment.getStatus())
                || NONE.equals(assessment.getStatus())
                || LOCKED.equals(assessment.getStatus()));
    String title = null;
    String message = null;
    String resend = null;
    if (pending) {
      title = BrdCopy.PENDING_TITLE;
      message = BrdCopy.PENDING_MESSAGE;
      resend = BrdCopy.RESEND;
    } else if (received) {
      title = BrdCopy.RECEIVED_TITLE;
      message = BrdCopy.RECEIVED_MESSAGE;
    }
    List<ProductView> products =
        assessment.isShowProducts()
            ? assessment.getProducts().stream().map(ProductView::from).toList()
            : List.of();
    DerivedView derived =
        assessment.getPremiumInr() == null
            ? null
            : new DerivedView(
                assessment.getPremiumInr(),
                assessment.getPremiumFrequency(),
                assessment.getPremiumPayingTerm(),
                assessment.getPolicyTerm(),
                assessment.getDerivationRuleId(),
                assessment.getUpdatedAt());
    return new AssessmentView(
        assessment.getAssessmentId(),
        assessment.getLeadId(),
        assessment.getJourneyId(),
        assessment.getCustomerId(),
        assessment.getProductType(),
        assessment.getStatus(),
        assessment.getLastScreen(),
        BrdCopy.screenTitle(assessment.getLastScreen(), assessment.getProposerName()),
        assessment.isAwaitingConsent(),
        assessment.getProposerName(),
        new ConsentPanel(
            assessment.getConsentState(),
            title,
            message,
            resend,
            assessment.getMobileMasked(),
            assessment.getEmailMasked()),
        new ExitCopy(BrdCopy.EXIT_TITLE, BrdCopy.EXIT_MESSAGE),
        products,
        derived,
        assessment.getNotice(),
        NONE.equals(assessment.getStatus()) ? BrdCopy.VAL_029 : null,
        AnswerSheet.asMap(assessment.getAnswers(), mapper));
  }

  private static String ulid(String field, String value) {
    if (!Ulids.isValid(value)) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", null, field, field + " must be a ULID");
    }
    return value;
  }

  private static String actor(String actorId) {
    if (actorId == null || actorId.isBlank() || actorId.trim().length() > 64) {
      throw new SuitabilityRefusal("UNAUTHORIZED", "actor is required");
    }
    return actorId.trim();
  }

  private static void requireActor(String actorId, String field) {
    if (actorId == null || actorId.isBlank() || actorId.trim().length() > 64) {
      throw new SuitabilityRefusal("VALIDATION_ERROR", null, field, field + " is required");
    }
  }

  public record CreateCommand(
      String leadId,
      String journeyId,
      String customerId,
      String productType,
      String proposerName,
      String proposerDob,
      String registeredMobile,
      String registeredEmail,
      String spActorId,
      String rmActorId) {}
}
