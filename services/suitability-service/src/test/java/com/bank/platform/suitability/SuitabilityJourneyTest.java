package com.bank.platform.suitability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.platform.suitability.domain.BrdCopy;
import com.bank.platform.suitability.domain.Ulids;
import com.bank.platform.suitability.support.FakeConsentLinks;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "suitability.journey.consent-client=fake")
@AutoConfigureMockMvc
class SuitabilityJourneyTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FakeConsentLinks consent;

  @BeforeEach
  void reset() {
    consent.reset();
  }

  @Test
  void savingsSelfJourneyWaitsForConsentThenOpensProducts() throws Exception {
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, profile("Saving and growing money"));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.awaitingConsent").value(true))
        .andExpect(jsonPath("$.lastScreen").value("CUSTOMER_CONSENT"))
        .andExpect(jsonPath("$.products").isEmpty())
        .andExpect(jsonPath("$.consent.title").value(BrdCopy.PENDING_TITLE))
        .andExpect(jsonPath("$.consent.message").value(BrdCopy.PENDING_MESSAGE))
        .andExpect(jsonPath("$.consent.resendLabel").value(BrdCopy.RESEND))
        .andExpect(jsonPath("$.consent.maskedMobile").value("+91 98*****210"))
        .andExpect(jsonPath("$.exit.title").value(BrdCopy.EXIT_TITLE))
        .andExpect(jsonPath("$.exit.message").value(BrdCopy.EXIT_MESSAGE))
        .andExpect(jsonPath("$.screenTitle").value(BrdCopy.PENDING_TITLE));
    org.assertj.core.api.Assertions.assertThat(consent.issues).isEqualTo(1);

    mvc.perform(get("/internal/v1/suitability/assessments/" + id).header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products").isEmpty())
        .andExpect(jsonPath("$.awaitingConsent").value(true));

    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/consent-link:resend")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.consent.resendLabel").value(BrdCopy.RESEND));
    org.assertj.core.api.Assertions.assertThat(consent.issues).isEqualTo(2);

    consent.state = "AGREED";
    mvc.perform(get("/internal/v1/suitability/assessments/" + id).header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.awaitingConsent").value(false))
        .andExpect(jsonPath("$.products[0].productId").value("SAV-BAL-01"))
        .andExpect(jsonPath("$.consent.title").value(BrdCopy.RECEIVED_TITLE))
        .andExpect(jsonPath("$.consent.message").value(BrdCopy.RECEIVED_MESSAGE))
        .andExpect(jsonPath("$.screenTitle").value("Suitable products"))
        .andExpect(jsonPath("$.derived.premiumInr").value(50000))
        .andExpect(jsonPath("$.derived.ruleId").value("INTERIM-BRD-22"))
        .andExpect(jsonPath("$.derived.premiumPayingTerm").value("10"))
        .andExpect(jsonPath("$.derived.policyTerm").value("20"))
        .andExpect(jsonPath("$.derived.frequency").value("ANNUAL"));
  }

  @Test
  void exploreBeforeAnswersReturnsVal001AndDoesNotIssueALink() throws Exception {
    String id = create("SAVINGS_PLAN", "1990-01-15");
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].code").value("VAL-001"))
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.VAL_001));
    org.assertj.core.api.Assertions.assertThat(consent.issues).isZero();
  }

  @Test
  void termIsRejectedAndMissingProposerDobIsVal026() throws Exception {
    mvc.perform(
            post("/internal/v1/suitability/assessments")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(createBody("TERM", "1990-01-15"))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.PRODUCT_TYPE));
    mvc.perform(
            post("/internal/v1/suitability/assessments")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(createBody("SAVINGS_PLAN", ""))))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].code").value("VAL-026"))
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.VAL_026));
  }

  @Test
  void nonSelfMissingNameIsVal002() throws Exception {
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, Map.of("screen", "POLICY_FOR", "policyFor", "Spouse"));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].code").value("VAL-002"))
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.VAL_002));
  }

  @Test
  void existingGrantMapsWithoutIssuingALink() throws Exception {
    consent.granted = true;
    String id = create("ULIP", "1990-01-15");
    Map<String, Object> answers = new LinkedHashMap<>(profile("Investing for higher returns"));
    answers.put("risk", "High returns");
    save(id, answers);
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.products[0].productId").value("ULIP-RET-01"))
        .andExpect(jsonPath("$.consent.title").value(BrdCopy.RECEIVED_TITLE));
    org.assertj.core.api.Assertions.assertThat(consent.issues).isZero();
  }

  @Test
  void healthGoalFindsNoProduct() throws Exception {
    consent.granted = true;
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, profile(BrdCopy.GOAL_HEALTH));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NO_SUITABLE_PRODUCT_FOUND"))
        .andExpect(jsonPath("$.products").isEmpty())
        .andExpect(jsonPath("$.noProductMessage").value(BrdCopy.VAL_029));
  }

  @Test
  void secondActorIsBlockedUntilSaveExit() throws Exception {
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, Map.of("screen", "LIFE_STAGE", "lifeStage", "Married"));
    mvc.perform(
            put("/internal/v1/suitability/assessments/" + id + "/answers")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("screen", "RISK", "risk", "Balance"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errors[0].code").value("VAL-031"))
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.VAL_031));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/save-exit")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exit.title").value(BrdCopy.EXIT_TITLE));
    mvc.perform(
            put("/internal/v1/suitability/assessments/" + id + "/answers")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("screen", "RISK", "risk", "Balance"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answers.lifeStage").value("Married"))
        .andExpect(jsonPath("$.answers.risk").value("Balance"));
  }

  @Test
  void answerChangeClearsProductsAndKeepsTheGrant() throws Exception {
    consent.granted = true;
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, profile("Saving and growing money"));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    save(id, Map.of("screen", "GOAL", "goal", BrdCopy.GOAL_HEALTH));
    mvc.perform(get("/internal/v1/suitability/assessments/" + id).header("X-Actor-Id", "sp-1"))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.products").isEmpty())
        .andExpect(jsonPath("$.notice").value(BrdCopy.VAL_032))
        .andExpect(jsonPath("$.awaitingConsent").value(false));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NO_SUITABLE_PRODUCT_FOUND"));
    org.assertj.core.api.Assertions.assertThat(consent.issues).isZero();
  }

  @Test
  void finalBiRejectsALaterEdit() throws Exception {
    consent.granted = true;
    String id = create("SAVINGS_PLAN", "1990-01-15");
    save(id, profile("Saving and growing money"));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/explore-matching-plans")
                .header("X-Actor-Id", "sp-1"))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    mvc.perform(
            post("/internal/v1/suitability/assessments/" + id + "/final-bi")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("biReference", "BI-100"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED_AND_LOCKED"));
    mvc.perform(
            put("/internal/v1/suitability/assessments/" + id + "/answers")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("lifeStage", "Single"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ASSESSMENT_IMMUTABLE"))
        .andExpect(jsonPath("$.errors[0].message").value(BrdCopy.VAL_030));
  }

  private String create(String productType, String dob) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/internal/v1/suitability/assessments")
                    .header("X-Actor-Id", "sp-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(createBody(productType, dob))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.screenTitle").value("Who are we planning this policy for?"))
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).path("assessmentId").asText();
  }

  private void save(String id, Map<String, Object> answers) throws Exception {
    mvc.perform(
            put("/internal/v1/suitability/assessments/" + id + "/answers")
                .header("X-Actor-Id", "sp-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(answers)))
        .andExpect(status().isOk());
  }

  private Map<String, Object> createBody(String productType, String dob) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("leadId", Ulids.mint(Clock.systemUTC()));
    body.put("journeyId", Ulids.mint(Clock.systemUTC()));
    body.put("customerId", Ulids.mint(Clock.systemUTC()));
    body.put("productType", productType);
    body.put("proposerName", "Asha Rao");
    body.put("proposerDob", dob);
    body.put("registeredMobile", "9876543210");
    body.put("registeredEmail", "asha@example.com");
    body.put("spActorId", "sp-1");
    body.put("rmActorId", "rm-1");
    return body;
  }

  private static Map<String, Object> profile(String goal) {
    Map<String, Object> answers = new LinkedHashMap<>();
    answers.put("screen", "PROPOSER_PROFILE");
    answers.put("policyFor", "Self");
    answers.put("lifeStage", "Married");
    answers.put("risk", "Balance");
    answers.put("goal", goal);
    answers.put("occupation", "Salaried");
    answers.put("education", "Graduate");
    answers.put("incomeBand", BrdCopy.BAND_5_10);
    return answers;
  }
}
