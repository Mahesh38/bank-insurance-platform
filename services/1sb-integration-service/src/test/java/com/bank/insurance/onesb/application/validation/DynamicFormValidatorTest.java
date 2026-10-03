package com.bank.insurance.onesb.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.domain.Lob;
import com.bank.common.domain.ProposalSchema;
import com.bank.common.error.ErrorCodes;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FUNC-028")
@Tag("unit")
class DynamicFormValidatorTest {

  @Test
  void usability_rejectsEmptySchemaAndEnumWithoutOptions() {
    assertThat(DynamicFormValidator.usabilityErrors(null))
        .anyMatch(e -> ErrorCodes.SCHEMA_INVALID.equals(e.code()));

    ProposalSchema schema =
        new ProposalSchema(
            Lob.SAVING,
            "P1",
            "MFG",
            "1",
            Map.of(
                "fields",
                List.of(
                    Map.of(
                        "id", "tobacco",
                        "mandatory", true,
                        "type", "select"))));

    assertThat(DynamicFormValidator.usabilityErrors(schema))
        .anyMatch(e -> e.message().contains("no option codes"));
  }

  @Test
  void answers_rejectMissingMandatoryLabelInsteadOfCodeAndWrongType() {
    ProposalSchema schema =
        new ProposalSchema(
            Lob.TERM,
            "T1",
            "HDFC",
            "1",
            Map.of(
                "fields",
                List.of(
                    Map.of(
                        "id",
                        "occupation",
                        "mandatory",
                        true,
                        "type",
                        "select",
                        "options",
                        List.of(Map.of("code", "SALARIED", "label", "Salaried"))),
                    Map.of("id", "age", "mandatory", true, "type", "integer"),
                    Map.of(
                        "id",
                        "child",
                        "mandatory",
                        true,
                        "type",
                        "string",
                        "parent",
                        "occupation"))));

    assertThat(
            DynamicFormValidator.answerErrors(
                schema,
                Map.of(
                    "occupation", "Salaried",
                    "age", "not-a-number")))
        .anyMatch(e -> e.field().contains("occupation"))
        .anyMatch(e -> e.field().contains("age"));

    assertThat(
            DynamicFormValidator.answerErrors(
                schema,
                Map.of(
                    "occupation", "SALARIED",
                    "age", 40,
                    "child", "ok")))
        .isEmpty();

    assertThat(DynamicFormValidator.answerErrors(schema, Map.of("age", 40)))
        .anyMatch(e -> e.field().contains("occupation"))
        .noneMatch(e -> e.field() != null && e.field().contains("child"));
  }
}
