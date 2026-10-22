package com.bank.platform.adminconfig.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.platform.adminconfig.application.SeedImportService;
import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.bank.platform.adminconfig.domain.RuleSetVersionRepository;
import com.bank.platform.adminconfig.domain.RuleSetVersionStatus;
import com.bank.rules.engine.RuleSetSeeds;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * ADR-024 section 5 lifecycle over HTTP on H2: maker-checker, approver role by classification, test
 * cases gate publish, resolution by effective date, published immutability, outbox, seeds.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RuleSetControllerTest {

  private static final String BASE = "/internal/v1/rule-sets/";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private RuleSetVersionRepository repository;
  @Autowired private SeedImportService seeds;

  /** The HUB.ROUTING seed re-identified so each test owns its rule set and domain. */
  private static ObjectNode routing(String ruleSetId, String version, String effectiveFrom) {
    ObjectNode doc =
        (ObjectNode)
            Values.mapper()
                .valueToTree(
                    Values.fromJson(
                        readTree(
                            RuleSetSeeds.all().stream()
                                .filter(s -> s.resource().endsWith("HUB.ROUTING.json"))
                                .findFirst()
                                .orElseThrow()
                                .json())));
    doc.put("ruleSetId", ruleSetId);
    doc.put("domain", ruleSetId.replace('.', '_'));
    doc.put("version", version);
    doc.put("effectiveFrom", effectiveFrom);
    return doc;
  }

  private static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
    try {
      return Values.mapper().readTree(json);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ResultActions create(ObjectNode doc, String maker) throws Exception {
    return mvc.perform(
        post(BASE + doc.get("ruleSetId").asText() + "/versions")
            .header("X-Actor-Id", maker)
            .contentType(MediaType.APPLICATION_JSON)
            .content(doc.toString()));
  }

  private ResultActions action(String id, String v, String verb, String actor, String roles)
      throws Exception {
    var req =
        post(BASE + id + "/versions/" + v + ":" + verb)
            .header("X-Actor-Id", actor)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"remarks\":\"checked\"}");
    if (roles != null) {
      req.header("X-Actor-Roles", roles);
    }
    return mvc.perform(req);
  }

  private void publish(ObjectNode doc) throws Exception {
    String id = doc.get("ruleSetId").asText();
    String v = doc.get("version").asText();
    create(doc, "maker-1").andExpect(status().isCreated());
    action(id, v, "submit", "maker-1", null).andExpect(status().isOk());
    action(id, v, "approve", "checker-1", "CONFIG_APPROVER")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  @Test
  void seedsAreImportedAsDraftsAndNeverPublished() {
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM administration.rule_set_version WHERE maker = ?",
                Integer.class,
                SeedImportService.SEED_MAKER))
        .isEqualTo(RuleSetSeeds.all().size());
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM administration.rule_set_version"
                    + " WHERE maker = ? AND status <> 'DRAFT'",
                Integer.class,
                SeedImportService.SEED_MAKER))
        .isZero();
    assertThat(seeds.importAbsent(RuleSetSeeds.all())).isEmpty();
  }

  @Test
  void publishWritesRuleSetPublishedToTheOutbox() throws Exception {
    publish(routing("T.PUBLISH", "1.0.0", "2026-11-01T00:00:00+05:30"));
    String payload =
        jdbc.queryForObject(
            "SELECT payload FROM administration.outbox_event WHERE aggregate_id = 'T.PUBLISH'",
            String.class);
    var env = Values.mapper().readTree(payload);
    assertThat(env.get("eventType").asText()).isEqualTo("RuleSetPublished");
    assertThat(env.get("eventId").asText()).matches("^[0-9A-HJKMNP-TV-Z]{26}$");
    assertThat(env.at("/data/version").asText()).isEqualTo("1.0.0");
    assertThat(env.get("configVersionRef").asText()).isEqualTo("T.PUBLISH@1.0.0");
    mvc.perform(get(BASE + "T.PUBLISH/versions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].checker").value("checker-1"));
  }

  @Test
  void makerCannotApproveTheirOwnVersion() throws Exception {
    ObjectNode doc = routing("T.MAKER", "1.0.0", "2026-11-01T00:00:00+05:30");
    create(doc, "alice").andExpect(status().isCreated());
    action("T.MAKER", "1.0.0", "submit", "alice", null).andExpect(status().isOk());
    action("T.MAKER", "1.0.0", "approve", "alice", "CONFIG_APPROVER")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("MAKER_CHECKER_VIOLATION"));
  }

  @Test
  void checkerNeedsTheRoleOfTheClassificationAndTheRolesHeader() throws Exception {
    ObjectNode doc = routing("T.ROLE", "1.0.0", "2026-11-01T00:00:00+05:30");
    doc.put("classification", "COMPLIANCE");
    create(doc, "maker-1").andExpect(status().isCreated());
    action("T.ROLE", "1.0.0", "submit", "maker-1", null).andExpect(status().isOk());
    action("T.ROLE", "1.0.0", "approve", "checker-1", "CONFIG_APPROVER,PRODUCT_APPROVER")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    action("T.ROLE", "1.0.0", "approve", "checker-1", null)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    action("T.ROLE", "1.0.0", "approve", "checker-1", "COMPLIANCE_APPROVER")
        .andExpect(status().isOk());
  }

  @Test
  void aFailingTestCaseBlocksPublish() throws Exception {
    ObjectNode doc = routing("T.TESTFAIL", "1.0.0", "2026-11-01T00:00:00+05:30");
    ((ObjectNode) doc.at("/testCases/0/expect")).put("outcome", "SOMEWHERE_ELSE");
    create(doc, "maker-1").andExpect(status().isCreated());
    action("T.TESTFAIL", "1.0.0", "submit", "maker-1", null).andExpect(status().isOk());
    action("T.TESTFAIL", "1.0.0", "approve", "checker-1", "CONFIG_APPROVER")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("RULE_SET_TEST_FAILED"));
    mvc.perform(get(BASE + "T.TESTFAIL/versions/1.0.0"))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    mvc.perform(
            post(BASE + "T.TESTFAIL/versions/1.0.0:simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inputs\":{\"lob\":\"LIFE\",\"productClass\":\"TERM\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allTestCasesPassed").value(false))
        .andExpect(jsonPath("$.evaluation.outcome").value("ONE_SB"));
  }

  @Test
  void resolvePicksTheVersionInForce() throws Exception {
    publish(routing("T.RESOLVE", "1.0.0", "2026-11-01T00:00:00+05:30"));
    ObjectNode v2 = routing("T.RESOLVE", "1.1.0", "2027-01-01T00:00:00+05:30");
    ((ObjectNode) v2.at("/parameters/r0")).put("provider", "ONE_SB_V2");
    for (var tc : (ArrayNode) v2.get("testCases")) {
      if (tc.at("/expect/outcome").asText().equals("ONE_SB")) {
        ((ObjectNode) tc.get("expect")).put("outcome", "ONE_SB_V2");
      }
    }
    publish(v2);
    mvc.perform(resolveAt("2026-12-01T00:00:00+05:30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value("1.0.0"))
        .andExpect(jsonPath("$.document.ruleSetId").value("T.RESOLVE"));
    mvc.perform(resolveAt("2027-02-01T00:00:00Z")).andExpect(jsonPath("$.version").value("1.1.0"));
    mvc.perform(resolveAt("2026-01-01T00:00:00Z"))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.code").value("CONFIGURATION_UNRESOLVABLE"));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      resolveAt(String asOf) {
    return get("/internal/v1/rule-sets:resolve")
        .param("domain", "T_RESOLVE")
        .param("lob", "LIFE")
        .param("productClass", "TERM")
        .param("asOf", asOf);
  }

  @Test
  void aPublishedVersionIsImmutable() throws Exception {
    ObjectNode doc = routing("T.IMMUTABLE", "1.0.0", "2026-11-01T00:00:00+05:30");
    publish(doc);
    doc.put("description", "changed");
    mvc.perform(
            put(BASE + "T.IMMUTABLE/versions/1.0.0")
                .header("X-Actor-Id", "maker-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(doc.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
    create(doc, "maker-1").andExpect(status().isConflict());
    RuleSetVersion stored = repository.find("T.IMMUTABLE", "1.0.0").orElseThrow();
    assertThatThrownBy(() -> repository.update(stored, RuleSetVersionStatus.PUBLISHED))
        .hasMessageContaining("immutable");
  }

  @Test
  void rejectNeedsRemarksAndSendsTheVersionBackForEditing() throws Exception {
    ObjectNode doc = routing("T.REJECT", "1.0.0", "2026-11-01T00:00:00+05:30");
    create(doc, "maker-1").andExpect(status().isCreated());
    action("T.REJECT", "1.0.0", "submit", "maker-1", null).andExpect(status().isOk());
    mvc.perform(
            post(BASE + "T.REJECT/versions/1.0.0:reject")
                .header("X-Actor-Id", "checker-1")
                .header("X-Actor-Roles", "CONFIG_APPROVER"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_FIELD"));
    action("T.REJECT", "1.0.0", "reject", "checker-1", "CONFIG_APPROVER")
        .andExpect(jsonPath("$.status").value("REJECTED"));
    action("T.REJECT", "1.0.0", "submit", "maker-1", null).andExpect(status().isConflict());
    mvc.perform(
            put(BASE + "T.REJECT/versions/1.0.0")
                .header("X-Actor-Id", "maker-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(doc.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.maker").value("maker-2"));
  }

  @Test
  void invalidOrMismatchedDocumentsAreRefused() throws Exception {
    ObjectNode doc = routing("T.INVALID", "1.0.0", "2026-11-01T00:00:00+05:30");
    ((ObjectNode) doc.at("/rules/0/when")).put("op", "eval");
    create(doc, "maker-1")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("RULE_SET_INVALID"));
    mvc.perform(
            post(BASE + "OTHER.ID/versions")
                .header("X-Actor-Id", "maker-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(routing("T.INVALID", "1.0.0", "2026-11-01T00:00:00Z").toString()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    mvc.perform(get(BASE + "NOPE/versions/9.9.9")).andExpect(status().isNotFound());
  }
}
